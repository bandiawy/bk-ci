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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.plugin.utils

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.plugin.codecc.CodeccUtils
import com.tencent.devops.plugin.codecc.config.CodeccConfig
import com.tencent.devops.store.api.atom.ServiceMarketAtomResource
import org.apache.commons.lang3.StringUtils

object ElementUtils {

    private lateinit var codeccV3AtomCode: String

    fun getElementCnName(classType: String, projectId: String): String {
        val map = getProjectElement(projectId)

        if (CodeccUtils.isCodeccAtom(classType)) {
            return map[getCodeCCV3AtomCode()] ?: ""
        }

        return map[classType] ?: ""
    }

    fun getCodeCCV3AtomCode(): String {
        if (StringUtils.isBlank(codeccV3AtomCode)) {
            codeccV3AtomCode = SpringContextUtil.getBean(CodeccConfig::class.java).codeccV3Atom
        }
        return codeccV3AtomCode
    }

    private fun getProjectElement(projectId: String): Map<String/* atomCode */, String/* cnName */> {
        val client = SpringContextUtil.getBean(Client::class.java)
        return client.get(ServiceMarketAtomResource::class).getProjectElements(projectId).data!!
    }
}