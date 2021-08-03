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

package com.tencent.devops.common.pipeline.pojo

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.pipeline.enums.ManualReviewAction
import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParam
import java.time.LocalDateTime

data class StagePauseCheck (
    val manualTrigger: Boolean? = false,
    var reviewStatus: String? = null,
    var reviewGroups: MutableList<StageReviewGroup>? = null, // 审核流配置
    val timeout: Int? = null, // 等待审核的超时时间
    var ruleIds: List<String>? = null // 质量红线规则ID集合
) {

    /**
     * 获取当前等待中的审核组
     */
    fun groupToReview(): StageReviewGroup? {
        reviewGroups?.forEach { group ->
            if (group.status.isNullOrBlank()) {
                return group
            }
        }
        return null
    }

    /**
     * 判断操作用户在不在当前审核人员名单中
     */
    fun reviewerContains(userId: String): Boolean {
        reviewGroups?.forEach { group ->
            if (group.status == null) {
                return group.reviewers.contains(userId)
            }
        }
        return false
    }

    /**
     * 审核通过当前等待中的审核组
     */
    fun reviewGroup(
        userId: String,
        action: ManualReviewAction,
        groupId: String? = null,
        params: List<ManualReviewParam>? = null,
        suggest: String? = null
    ): Boolean {
        val group = getReviewGroupById(groupId) ?: return false
        if (group.status == null) {
            group.status = action.name
            group.operator = userId
            group.reviewTime = LocalDateTime.now().timestampmilli()
            group.params = params?.toMutableList()
            group.suggest = suggest
            return true
        }
        return false
    }



    /**
     * 获取指定ID的审核组
     */
    fun getReviewGroupById(groupId: String?): StageReviewGroup? {
        // #4531 兼容旧的前端交互，如果是不带ID审核参数则默认返回第一个审核组（旧数据）
        if (groupId.isNullOrBlank()) return reviewGroups?.first()
        reviewGroups?.forEach { group ->
            if (group.id == groupId) {
                return group
            }
        }
        return null
    }

    /**
     * 填充审核组ID
     */
    private fun fixReviewGroups() {
        reviewGroups?.forEach { group ->
            if (group.id.isNullOrBlank()) group.id = UUIDUtil.generate()
        }
    }
}
