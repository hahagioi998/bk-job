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
    <layout class="script-manage-detail-box">
        <div slot="title">
            <span>{{ scriptInfo.version }}</span>
            <span class="script-status" v-html="scriptInfo.statusHtml" />
        </div>
        <template slot="sub-header">
            <span style="font-size: 12px; color: #63656e;">
                <span>{{ scriptInfo.lastModifyUser }}</span>
                <span>|</span>
                <span>{{ scriptInfo.lastModifyTime }}</span>
            </span>
            <Icon
                v-if="contentTab === 'content'"
                type="full-screen"
                v-bk-tooltips="$t('全屏')"
                @click="handleFullScreen"
                v-test="{ type: 'button', value: 'scriptEditFullscreen' }" />
        </template>
        <div class="content-tab">
            <div
                class="content-tab-item"
                :class="{ active: contentTab === 'content' }"
                @click="handleChangeDispaly('content')"
                v-test="{ type: 'button', value: 'scriptContentTab' }">
                {{ $t('script.脚本内容') }}
            </div>
            <div
                class="content-tab-item"
                :class="{ active: contentTab === 'log' }"
                @click="handleChangeDispaly('log')"
                v-test="{ type: 'button', value: 'scriptVersionLogTab' }">
                {{ $t('script.版本日志') }}
            </div>
        </div>
        <div class="version-content">
            <div ref="content" class="content-dispaly" :style="contentStyles">
                <keep-alive v-if="contentHeight > 0">
                    <component
                        ref="aceEditor"
                        :is="contentCom"
                        readonly
                        :height="contentHeight"
                        :value="scriptInfo.content"
                        :lang="scriptInfo.typeName"
                        :options="scriptInfo.typeName"
                        :version-desc="scriptInfo.versionDesc" />
                </keep-alive>
            </div>
        </div>
        <template #footer>
            <auth-button
                v-if="scriptInfo.isOnline"
                key="execute"
                :permission="scriptInfo.canManage"
                auth="script/execute"
                :resource-id="scriptInfo.id"
                class="w120 mr10"
                theme="primary"
                :loading="isExceLoading"
                @click="handleGoExce"
                v-test="{ type: 'button', value: 'execScript' }">
                {{ $t('script.去执行') }}
            </auth-button>
            <jb-popover-confirm
                v-if="!scriptInfo.isOnline"
                key="online"
                class="mr10"
                :title="$t('script.确定上线该版本？')"
                :content="$t('script.上线后，之前的线上版本将被置为「已下线」状态，但不影响作业使用')"
                :disabled="scriptInfo.isDisabledOnline"
                :confirm-handler="handleOnline">
                <auth-button
                    :permission="scriptInfo.canManage"
                    :resource-id="scriptInfo.id"
                    auth="script/edit"
                    theme="primary"
                    class="w120"
                    :disabled="scriptInfo.isDisabledOnline"
                    v-test="{ type: 'button', value: 'onlineScript' }">
                    {{ $t('script.上线') }}
                </auth-button>
            </jb-popover-confirm>
            <span
                v-if="!scriptInfo.isDraft"
                key="create"
                :tippy-tips="isCopyCreateDisabled ? $t('script.已有[未上线]版本') : ''">
                <auth-button
                    :permission="scriptInfo.canClone"
                    :resource-id="scriptInfo.id"
                    auth="script/clone"
                    :disabled="isCopyCreateDisabled"
                    class="w120 mr10"
                    @click="handleCopyAndCreate(scriptInfo)"
                    v-test="{ type: 'button', value: 'copyCreateScript' }">
                    {{ $t('script.复制并新建') }}
                </auth-button>
            </span>
            <bk-button
                class="mr10"
                @click="handleDebugScript"
                v-test="{ type: 'button', value: 'debugScript' }">
                {{ $t('script.调试') }}
            </bk-button>
            <span
                v-if="scriptInfo.isOnline"
                key="sync"
                class="mr10"
                :tippy-tips="!scriptInfo.syncEnabled ? $t('script.所有关联作业模板已是当前版本') : ''">
                <auth-button
                    :permission="scriptInfo.canManage"
                    :resource-id="scriptInfo.id"
                    auth="script/edit"
                    :disabled="!scriptInfo.syncEnabled"
                    @click="handleGoSync"
                    v-test="{ type: 'button', value: 'syncScript' }">
                    {{ $t('script.同步') }}
                </auth-button>
            </span>
            <auth-button
                v-if="scriptInfo.isDraft"
                key="edit"
                :permission="scriptInfo.canManage"
                :resource-id="scriptInfo.id"
                auth="script/edit"
                class="mr10"
                @click="handleEdit(scriptInfo)"
                v-test="{ type: 'button', value: 'editScript' }">
                {{ $t('script.编辑') }}
            </auth-button>
            <jb-popover-confirm
                v-if="scriptInfo.isVersionEnableRemove"
                key="delete"
                class="mr10"
                :title="$t('script.确定删除该版本？')"
                :content="$t('script.删除后不可恢复，请谨慎操作！')"
                :confirm-handler="handleRemove">
                <auth-button
                    :permission="scriptInfo.canManage"
                    :resource-id="scriptInfo.id"
                    auth="script/delete"
                    v-test="{ type: 'button', value: 'deleteScript' }">
                    {{ $t('script.删除') }}
                </auth-button>
            </jb-popover-confirm>
            <jb-popover-confirm
                v-if="scriptInfo.isOnline"
                key="offline"
                style="margin-left: auto;"
                :title="$t('script.确定禁用该版本？')"
                :content="$t('script.一旦禁用成功，线上引用该版本的作业脚本步骤都会执行失败，请务必谨慎操作！')"
                :confirm-handler="handleOffline">
                <auth-button
                    :permission="scriptInfo.canManage"
                    :resource-id="scriptInfo.id"
                    auth="script/edit"
                    v-test="{ type: 'button', value: 'offlineScript' }">
                    {{ $t('script.禁用') }}
                </auth-button>
            </jb-popover-confirm>
        </template>
    </layout>
</template>
<script>
    import _ from 'lodash';
    import I18n from '@/i18n';
    import ScriptService from '@service/script-manage';
    import PublicScriptService from '@service/public-script-manage';
    import {
        checkPublicScript,
        getOffset,
    } from '@utils/assist';
    import { debugScriptCache } from '@utils/cache-helper';
    import AceEditor from '@components/ace-editor';
    import DetailLayout from '@components/detail-layout';
    import DetailItem from '@components/detail-layout/item';
    import JbPopoverConfirm from '@components/jb-popover-confirm';
    import Layout from '../components/layout';
    import RenderLog from './components/render-log';

    export default {
        name: '',
        components: {
            AceEditor,
            DetailLayout,
            DetailItem,
            JbPopoverConfirm,
            Layout,
            RenderLog,
        },
        inheritAttrs: false,
        props: {
            scriptVersionList: {
                type: Array,
                default: () => [],
            },
            scriptInfo: {
                type: Object,
                required: true,
            },
        },
        data () {
            return {
                isLoading: false,
                isExceLoading: false,
                contentTab: 'content',
                contentHeight: 0,
            };
        },
        computed: {
            contentCom () {
                const comMap = {
                    content: AceEditor,
                    log: RenderLog,
                };
                return comMap[this.contentTab];
            },
            /**
             * @desc 已存在未上线版本不允许新建版本
             * @returns { Boolean }
             */
            isCopyCreateDisabled () {
                return !!_.find(this.scriptVersionList, scriptVersion => scriptVersion.isDraft);
            },
            contentStyles () {
                return {
                    height: `${this.contentHeight}px`,
                };
            },
        },
        watch: {
            scriptInfo () {
                this.contentTab = 'content';
            },
        },
        created () {
            this.publicScript = checkPublicScript(this.$route);
            this.serviceHandler = this.publicScript ? PublicScriptService : ScriptService;
            window.addEventListener('resize', this.init);
            this.$once('hook:beforeDestroy', () => {
                window.removeEventListener('resize', this.init);
            });
        },
        mounted () {
            this.calcContentHeight();
            const handleResize = _.throttle(this.calcContentHeight, 60);
            window.addEventListener('resize', handleResize);
            this.$once('hook:beforeDestroy', () => {
                window.removeEventListener('resize', handleResize);
            });
        },
        methods: {
            /**
             * @desc 计算内容区高度
             */
            calcContentHeight () {
                const contentOffsetTop = getOffset(this.$refs.content).top;
                this.contentHeight = window.innerHeight - contentOffsetTop - 20;
            },
            handleFullScreen () {
                if (this.contentTab === 'content') {
                    this.$refs.aceEditor.handleFullScreen();
                }
            },
            /**
             * @desc 脚本内容和脚本版本日志切换
             * @param {String} tab 切换面板
             */
            handleChangeDispaly (tab) {
                this.contentTab = tab;
            },
            /**
             * @desc 上线脚本
             */
            handleOnline () {
                return this.serviceHandler.scriptVersionOnline({
                    id: this.scriptInfo.id,
                    versionId: this.scriptInfo.scriptVersionId,
                }).then(() => {
                    this.messageSuccess(I18n.t('script.操作成功'));
                    this.$emit('on-edit-change');
                });
            },
            /**
             * @desc 删除脚本
             */
            handleRemove () {
                return this.serviceHandler.scriptVersionRemove({
                    versionId: this.scriptInfo.scriptVersionId,
                }).then(() => {
                    this.messageSuccess(I18n.t('script.删除成功'));
                    // 如果删除的是最后一个版本，成功后跳转到脚本列表
                    if (this.scriptVersionList.length < 2) {
                        const routerName = this.publicScript ? 'publicScriptList' : 'scriptList';
                        this.$router.push({
                            name: routerName,
                        });
                    } else {
                        setTimeout(() => {
                            this.$emit('on-delete', true);
                        });
                    }
                });
            },
            /**
             * @desc 下线脚本
             */
            handleOffline () {
                return this.serviceHandler.scriptVersionOffline({
                    id: this.scriptInfo.id,
                    versionId: this.scriptInfo.scriptVersionId,
                }).then(() => {
                    this.messageSuccess(I18n.t('script.操作成功'));
                    this.$emit('on-edit-change');
                });
            },
            /**
             * @desc 复制新建
             */
            handleCopyAndCreate () {
                this.$emit('on-go-copy-create', {
                    scriptVersionId: this.scriptInfo.scriptVersionId,
                });
            },
            /**
             * @desc 调整到快速执行脚本页面调试脚本
             */
            handleDebugScript () {
                debugScriptCache.setItem(this.scriptInfo.content);
                const { href } = this.$router.resolve({
                    name: 'fastExecuteScript',
                    query: {
                        model: 'debugScript',
                    },
                });
                window.open(href);
            },
            /**
             * @desc 同步脚本
             */
            handleGoSync () {
                const routerName = this.publicScript ? 'scriptPublicSync' : 'scriptSync';
                this.$router.push({
                    name: routerName,
                    params: {
                        scriptId: this.scriptInfo.id,
                        scriptVersionId: this.scriptInfo.scriptVersionId,
                    },
                });
            },
            /**
             * @desc 编辑脚本
             */
            handleEdit (payload) {
                this.$emit('on-go-edit', {
                    scriptVersionId: this.scriptInfo.scriptVersionId,
                });
            },
            /**
             * @desc 调整到快速执行脚本页面执行脚本
             */
            handleGoExce () {
                this.$router.push({
                    name: 'fastExecuteScript',
                    params: {
                        taskInstanceId: 0,
                        scriptVersionId: this.scriptInfo.scriptVersionId,
                    },
                    query: {
                        from: 'scriptVersion',
                    },
                });
            },
        },
    };
</script>
<style lang='postcss'>
    @import "@/css/mixins/scroll";

    %tab-item {
        display: flex;
        width: 94px;
        height: 35px;
        font-size: 13px;
        color: #979ba5;
        align-items: center;
    }

    .script-manage-detail-box {
        .content-tab {
            position: absolute;
            left: 50%;
            z-index: 0;
            display: flex;
            width: 250px;
            margin-top: -35px;
            transform: translateX(-50%);
            align-content: center;
            justify-content: center;

            .content-tab-item {
                @extend %tab-item;

                padding-left: 0;
                cursor: pointer;
                transition: all 0.1s;
                flex: 0 0 120px;
                align-items: center;
                justify-content: center;

                &.active {
                    color: #dcdee5;
                    background: #242424;
                    border-top-right-radius: 6px;
                    border-top-left-radius: 6px;
                }
            }
        }

        .version-content {
            display: flex;
            flex-direction: column;
            flex: 1;
        }

        .render-version-log {
            height: 100%;
            padding: 12px 10px;
            overflow-y: auto;
            font-size: 12px;
            line-height: 20px;
            color: #63656e;
            white-space: pre-line;

            @mixin scroller;
        }
    }
</style>
