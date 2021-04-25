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

package com.tencent.devops.plugin.codecc.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class CodeccConfig {

    /**
     * 代码检查网关地址
     */
    @Value("\${plugin.codecc.gateway:}")
    val codeccApiGateWay: String = ""

    @Value("\${plugin.codecc.proxy:}")
    val codeccApiProxyGateWay: String = ""

    @Value("\${plugin.codecc.api.createTask:/ms/task/api/service/task}")
    val createPath = "/ms/task/api/service/task"

    @Value("\${plugin.codecc.api.updateTask:/ms/task/api/service/task}")
    val updatePath = "/ms/task/api/service/task"

    @Value("\${plugin.codecc.api.checkTaskExists:/ms/task/api/service/task/exists}")
    val existPath = "/ms/task/api/service/task/exists"

    @Value("\${plugin.codecc.api.deleteTask:/ms/task/api/service/task}")
    val deletePath = "/ms/task/api/service/task"

    @Value("\${plugin.codecc.api.codeCheckReport:/api}")
    val report = ""

    @Value("\${plugin.codecc.api.getRuleSets:/blueShield/getRuleSetsPath}")
    val getRuleSetsPath = ""

    @Value("\${pipeline.atomCode.codeccV2:CodeccCheckAtom}")
    val codeccV2Atom = ""

    @Value("\${pipeline.atomCode.codecc:CodeccCheckAtomDebug}")
    val codeccV3Atom = ""
}