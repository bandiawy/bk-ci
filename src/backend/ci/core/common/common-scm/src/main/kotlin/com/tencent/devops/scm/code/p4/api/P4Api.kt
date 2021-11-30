/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.scm.code.p4.api

import com.perforce.p4java.admin.ITriggerEntry
import com.perforce.p4java.admin.ITriggerEntry.TriggerType
import com.perforce.p4java.core.IDepot
import com.perforce.p4java.core.file.FileSpecOpStatus
import com.tencent.devops.scm.code.p4.P4Client
import com.tencent.devops.scm.code.p4.P4Server
import com.tencent.devops.scm.pojo.p4.DepotInfo
import com.tencent.devops.scm.pojo.p4.TriggerInfo
import com.tencent.devops.scm.pojo.p4.Workspace
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import java.nio.file.Files
import java.nio.file.Paths
import java.text.MessageFormat
import java.time.LocalDateTime
import java.util.UUID

class P4Api(
    val p4port: String,
    val username: String,
    val password: String
) {

    companion object {
        private val logger = LoggerFactory.getLogger(P4Api::class.java)
        private const val DEVOPS_P4_TRIGGER_NAME = "devops"
        private const val DEVOPS_P4_TRIGGER_DEPOT_NAME = "devops_trigger"
        private const val DEVOPS_P4_TRIGGER_DEPOT_DESCRIPTION = "devops trigger script"
        private const val DEVOPS_P4_TRIGGER_WORKSPACE_NAME_PREFIX = "devops_"
        private const val DEVOPS_P4_TRIGGER_WORKSPACE_DESCRIPTION = "devops trigger workspace"
        private const val DEVOPS_P4_TRIGGER_SUBMIT_DESC = "devops add trigger"
    }

    fun connection() {
        P4Server(p4port = p4port, userName = username, password = password).use { p4Server ->
            p4Server.connectionRetry()
        }
    }

    fun addWebHook(
        hookUrl: String,
        includePaths: String?,
        excludePaths: String?,
        event: String?
    ) {
        P4Server(p4port = p4port, userName = username, password = password).use { p4Server ->
            p4Server.connectionRetry()
            val eventType = triggerTypeValueOf(event) ?: TriggerType.CHANGE_COMMIT
            val (eventScriptFileName, command) = getTriggerFileAndCommand(
                eventType = eventType,
                hookUrl = hookUrl
            )
            val remainPaths = filterExistTrigger(
                p4Server = p4Server,
                includePaths = includePaths,
                excludePaths = excludePaths,
                eventType = eventType,
                command = command
            )
            if (remainPaths.isEmpty()) {
                logger.info(
                    "The web hook url($hookUrl) and event($event)," +
                        "includePaths($includePaths),excludePaths($excludePaths) is already exist"
                )
                return
            }
            // 创建一个devops_trigger的depot
            createDevopsDepot(p4Server)

            // 如果事件脚本不存在,则创建
            p4Server.listFiles(DEVOPS_P4_TRIGGER_DEPOT_NAME).filter { it.opStatus == FileSpecOpStatus.VALID }
                .find { it.depotPathString == "//$DEVOPS_P4_TRIGGER_DEPOT_NAME/$eventScriptFileName" }
                ?: addDevopsScriptFile(
                    p4Server = p4Server,
                    sourceScriptFileName = "p4trigger/$eventScriptFileName",
                    eventScriptFileName = eventScriptFileName
                )

            addTriggers(
                p4Server = p4Server,
                remainPaths = remainPaths,
                eventType = eventType,
                command = command
            )
        }
    }

    private fun triggerTypeValueOf(event: String?): TriggerType? {
        if (event != null) {
            TriggerType.values().forEach { tt ->
                if (tt.name == event) {
                    return tt
                }
            }
        }
        return null
    }

    fun getChangelistFiles(change: Int): List<P4FileSpec> {
        return P4Server(p4port = p4port, userName = username, password = password).use { p4Server ->
            p4Server.connectionRetry()
            p4Server.getChangelistFiles(change = change)
        }.map { iFileSpec ->
            P4FileSpec(
                opStatus = iFileSpec.opStatus.name,
                depotPathString = iFileSpec.depotPathString
            )
        }
    }

    @SuppressWarnings("LongParameterList")
    private fun filterExistTrigger(
        p4Server: P4Server,
        includePaths: String?,
        excludePaths: String?,
        eventType: TriggerType,
        command: String
    ): Set<String> {
        val paths = mutableSetOf<String>()
        if (!includePaths.isNullOrBlank()) {
            paths.addAll(includePaths.split(","))
        }
        if (!excludePaths.isNullOrBlank()) {
            paths.addAll(excludePaths.split(",").map { "-$it" })
        }
        p4Server.getTriggers().forEach { entry ->
            if (isSameEvent(entry, paths, eventType, command)) {
                paths.remove(entry.path)
            }
        }
        return paths
    }

    private fun isSameEvent(
        entry: ITriggerEntry,
        paths: Set<String>,
        eventType: TriggerType,
        command: String
    ) = paths.contains(entry.path) &&
        entry.triggerType.name == eventType.name &&
        entry.command == command

    private fun createDevopsDepot(p4Server: P4Server) =
        p4Server.createDepot(
            DepotInfo(
                name = DEVOPS_P4_TRIGGER_DEPOT_NAME,
                ownerName = username,
                modDate = LocalDateTime.now(),
                description = DEVOPS_P4_TRIGGER_DEPOT_DESCRIPTION,
                depotType = IDepot.DepotType.LOCAL
            )
        )

    private fun addDevopsScriptFile(
        p4Server: P4Server,
        sourceScriptFileName: String,
        eventScriptFileName: String
    ) {
        val tmpPath = Files.createTempDirectory(DEVOPS_P4_TRIGGER_NAME)
        val eventScriptFilePath = Paths.get(tmpPath.toString(), eventScriptFileName)
        val workspaceName = DEVOPS_P4_TRIGGER_WORKSPACE_NAME_PREFIX + UUID.randomUUID().toString()
        val workspace = Workspace(
            name = workspaceName,
            description = DEVOPS_P4_TRIGGER_WORKSPACE_DESCRIPTION,
            root = tmpPath.toString(),
            mappings = arrayListOf("//$DEVOPS_P4_TRIGGER_DEPOT_NAME/... //$workspaceName/...")
        )
        ClassPathResource(sourceScriptFileName).inputStream.use { inputStream ->
            FileUtils.copyToFile(inputStream, eventScriptFilePath.toFile())
        }
        P4Client(server = p4Server.getServer(), workspace = workspace).use { p4Client ->
            p4Client.addFile(
                desc = DEVOPS_P4_TRIGGER_SUBMIT_DESC,
                path = eventScriptFilePath.toString()
            )
        }
    }

    private fun getTriggerFileAndCommand(
        eventType: TriggerType,
        hookUrl: String
    ): Pair<String, String> {
        val baseCommand = "%//$DEVOPS_P4_TRIGGER_DEPOT_NAME/{0}% $hookUrl $p4port ${eventType.name}"
        return when (eventType) {
            TriggerType.CHANGE_COMMIT,
            TriggerType.PUSH_SUBMIT,
            TriggerType.CHANGE_CONTENT,
            TriggerType.PUSH_CONTENT,
            TriggerType.PUSH_COMMIT,
            TriggerType.FIX_ADD,
            TriggerType.FIX_DELETE,
            TriggerType.FORM_COMMIT,
            TriggerType.SHELVE_COMMIT,
            TriggerType.SHELVE_DELETE,
            TriggerType.CHANGE_SUBMIT
            -> {
                val eventScriptFileName = "change.sh"
                val replaceCommand = MessageFormat.format(baseCommand, eventScriptFileName)
                Pair(eventScriptFileName, "\"$replaceCommand %change%\"")
            }
            else -> {
                val eventScriptFileName = "change.sh"
                val replaceCommand = MessageFormat.format(baseCommand, eventScriptFileName)
                Pair(eventScriptFileName, "\"$replaceCommand %change%\"")
            }
        }
    }

    private fun addTriggers(
        p4Server: P4Server,
        remainPaths: Set<String>,
        eventType: TriggerType,
        command: String
    ) {
        // p4触发器是按照顺序更新的,所以再次获取p4触发器数量，然后再增加
        val oldTriggers = p4Server.getTriggers().mapIndexed { index, entry ->
            with(entry) {
                TriggerInfo(
                    name = name,
                    type = triggerType,
                    path = entry.path,
                    command = entry.command,
                    order = index
                )
            }
        }
        var order = oldTriggers.size
        val newTriggers = mutableListOf<TriggerInfo>()
        newTriggers.addAll(oldTriggers)
        // 新增触发器
        remainPaths.forEach { path ->
            val trigger = TriggerInfo(
                name = "${DEVOPS_P4_TRIGGER_NAME}_$order",
                type = eventType,
                path = path,
                command = command,
                order = order++
            )
            newTriggers.add(trigger)
        }

        val result = p4Server.addTriggers(newTriggers)
        logger.info("add p4 triggers|$p4port|$remainPaths|$eventType|$result")
    }
}