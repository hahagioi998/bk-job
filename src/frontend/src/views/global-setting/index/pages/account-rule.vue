<!--
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
-->

<template>
    <div class="page-account-rule" v-bkloading="{ isLoading }">
        <smart-action offset-target="expression-input">
            <div class="wraper">
                <div v-for="(rule, index) in currentRules" :key="index" class="account-block">
                    <div class="name">{{ rule.osTypeText }}</div>
                    <div class="expression-input">
                        <bk-input
                            :value="rule.expression"
                            :placeholder="$t('setting.请输入命名规则')"
                            @change="value => handleChange('expression', value, index)" />
                    </div>
                    <div class="rule">
                        <bk-input
                            :value="rule.description"
                            :placeholder="$t('setting.请输入命名规则提醒文案')"
                            @change="value => handleChange('description', value, index)" />
                    </div>
                    <bk-button text class="reset" @click="handleReset(index)">{{ $t('setting.恢复默认') }}</bk-button>
                </div>
            </div>
            <template #action>
                <bk-button
                    theme="primary"
                    :loading="isSubmitting"
                    class="w120 mr10"
                    @click="handleSave">{{ $t('setting.保存') }}</bk-button>
                <bk-button @click="handleResetAll">{{ $t('setting.重置') }}</bk-button>
            </template>
        </smart-action>
    </div>
</template>
<script>
    import I18n from '@/i18n';
    import _ from 'lodash';
    import GlobalSettingService from '@service/global-setting';
    import SmartAction from '@components/smart-action';

    export default {
        name: '',
        components: {
            SmartAction,
        },
        data () {
            return {
                isLoading: false,
                isSubmitting: false,
                currentRules: [],
            };
        },
        created () {
            this.fetchData();
            this.defaultRules = [];
            this.selfLastRules = [];
        },
        methods: {
            fetchData () {
                this.isLoading = true;
                GlobalSettingService.fetchAllNameRule()
                    .then((data) => {
                        const { currentRules, defaultRules } = data;
                        this.currentRules = currentRules;
                        this.selfLastRules = _.cloneDeep(currentRules);
                        this.defaultRules = defaultRules;
                    })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            handleChange (field, value, index) {
                window.changeAlert = true;
                this.currentRules[index][field] = value;
                this.currentRules = [...this.currentRules];
            },
            handleReset (index) {
                const currentRule = _.cloneDeep(this.defaultRules[index]);
                this.currentRules.splice(index, 1, currentRule);
            },
            handleSave () {
                this.isSubmitting = true;
                GlobalSettingService.updateNameRules({
                    rules: this.currentRules,
                }).then(() => {
                    window.changeAlert = false;
                    this.messageSuccess(I18n.t('setting.账号命名规则保存成功'));
                })
                    .finally(() => {
                        this.isSubmitting = false;
                    });
            },
            handleResetAll () {
                this.currentRules = _.cloneDeep(this.selfLastRules);
            },
        },
    };
</script>
<style lang='postcss'>
    .page-account-rule {
        display: flex;
        justify-content: center;
        padding-top: 40px;
        padding-bottom: 40px;

        .wraper {
            margin-bottom: 30px;
        }

        .account-block {
            display: flex;
            align-items: center;
            justify-content: flex-end;
            margin-bottom: 20px;
            color: #63656e;

            .name {
                font-size: 14px;
            }

            .expression-input {
                width: 300px;
                margin-left: 24px;
            }

            .rule {
                width: 400px;
                margin: 0 14px 0 10px;
            }

            .reset {
                font-size: 12px;
            }
        }

        .action-box {
            margin-top: 30px;
        }
    }
</style>
