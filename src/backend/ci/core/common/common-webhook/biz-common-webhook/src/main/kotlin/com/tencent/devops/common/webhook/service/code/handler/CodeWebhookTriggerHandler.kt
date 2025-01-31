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

package com.tencent.devops.common.webhook.service.code.handler

import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.webhook.pojo.code.CodeWebhookEvent
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilter
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilterChain
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilterResponse
import com.tencent.devops.common.webhook.service.code.matcher.ScmWebhookMatcher
import com.tencent.devops.repository.pojo.Repository

@Suppress("TooManyFunctions")
interface CodeWebhookTriggerHandler<T : CodeWebhookEvent> {

    /**
     * 处理类是否能够处理
     */
    fun eventClass(): Class<T>

    fun getUrl(event: T): String

    fun getUsername(event: T): String

    fun getRevision(event: T): String

    fun getRepoName(event: T): String

    fun getBranchName(event: T): String

    fun getEventType(): CodeEventType

    fun getEventType(event: T): CodeEventType? = null

    fun getHookSourceUrl(event: T): String? = null

    fun getHookTargetUrl(event: T): String? = null

    fun getEnv(event: T): Map<String, Any> = emptyMap()

    fun getMergeRequestId(event: T): Long? = null

    fun getMessage(event: T): String?

    fun preMatch(event: T): ScmWebhookMatcher.MatchResult = ScmWebhookMatcher.MatchResult(isMatch = true)

    /**
     * 匹配事件
     */
    fun isMatch(
        event: T,
        projectId: String,
        pipelineId: String,
        repository: Repository,
        webHookParams: WebHookParams
    ): ScmWebhookMatcher.MatchResult {
        val filters = getWebhookFilters(
            event = event,
            projectId = projectId,
            pipelineId = pipelineId,
            repository = repository,
            webHookParams = webHookParams
        )
        val response = WebhookFilterResponse()
        return if (filters.isNotEmpty()) {
            ScmWebhookMatcher.MatchResult(
                isMatch = WebhookFilterChain(filters = filters).doFilter(response),
                extra = response.getParam()
            )
        } else {
            ScmWebhookMatcher.MatchResult(isMatch = true)
        }
    }

    fun getWebhookFilters(
        event: T,
        projectId: String,
        pipelineId: String,
        repository: Repository,
        webHookParams: WebHookParams
    ): List<WebhookFilter>

    fun retrieveParams(
        event: T,
        projectId: String? = null,
        repository: Repository? = null
    ): Map<String, Any>
}
