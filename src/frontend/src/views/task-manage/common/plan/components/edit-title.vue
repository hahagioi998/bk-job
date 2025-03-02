<template>
    <div ref="box" class="plan-edit-title" @click.stop="">
        <div
            v-if="isEditing"
            class="input-box"
            :class="{
                'validate-error': !!errorInfo,
            }">
            <jb-input
                ref="input"
                v-model="localValue"
                :style="{
                    width: `${inputWidth}px`,
                }"
                :native-attributes="{
                    spellcheck: false,
                    autofocus: true,
                }"
                :placeholder="$t('template.推荐按照该执行方案提供的使用场景来取名...')"
                enter-trigger
                :maxlength="60"
                behavior="simplicity"
                @input="handleInput"
                @submit="handleSubmit" />
            <i
                v-if="errorInfo"
                v-bk-tooltips="errorInfo"
                class="edit-status-flag bk-icon icon-exclamation-circle-shape"
                style="color: #ea3636;" />
            <Icon
                v-if="isSubmiting"
                type="loading-circle"
                class="edit-status-flag rotate-loading"
                style="color: #979ba5;" />
        </div>
        <div
            v-else
            ref="text"
            class="text">
            <span>{{ localValue }}</span>
            <Icon
                class="edit-btn"
                type="edit-2"
                @click="handleEdit" />
        </div>
    </div>
</template>
<script>
    import _ from 'lodash';
    import TaskPlanService from '@service/task-plan';
    import I18n from '@/i18n';
    import { calcTextWidth, getOffset } from '@utils/assist';
    import { planNameRule } from '@utils/validator';

    export default {
        name: '',
        props: {
            data: {
                type: Object,
                required: true,
            },
        },
        data () {
            return {
                isSubmiting: false,
                isEditing: false,
                inputWidth: 'auto',
                localValue: '',
                errorInfo: '',
            };
        },
        watch: {
            /**
             * @desc 更新执行方案的名称
             */
            data: {
                handler  () {
                    this.localValue = this.data.name || '';
                    this.localValueMemo = this.localValue;
                },
                immediate: true,
            },
        },
        mounted () {
            document.body.addEventListener('click', this.hideCallback);
            this.$once('hook:beforeDestroy', () => {
                document.body.removeEventListener('click', this.hideCallback);
            });
        },
        methods: {
            /**
             * @desc 切换编辑状态
             */
            hideCallback () {
                if (!this.isEditing) {
                    return;
                }
                this.handleSubmit();
            },
            /**
             * @desc 开始编辑
             */
            handleEdit () {
                this.isSubmiting = false;
                this.isEditing = true;
                this.errorInfo = '';
                this.inputWidth = this.$refs.text.getBoundingClientRect().width;
                // 输入框自动获取焦点
                setTimeout(() => {
                    this.$refs.input.$el.querySelector('.bk-form-input').focus();
                });
            },
            /**
             * @desc 用户输入时自适应输入框宽度
             * @param { String } value 输入值
             */
            handleInput: _.throttle(function (value) {
                const windowClienWidth = window.innerWidth;
                const offset = 60;
                const width = calcTextWidth(value, this.$refs.box) + offset;
                const { left } = getOffset(this.$refs.box);
                const maxWidth = windowClienWidth - left - 230;
                if (width <= maxWidth && width > this.inputWidth) {
                    this.inputWidth = width;
                }
            }, 60),
            /**
             * @desc 提交编辑值
             */
            handleSubmit () {
                this.errorInfo = '';
                // 值没变
                if (this.localValueMemo === this.localValue) {
                    this.isEditing = false;
                    return;
                }
                // 值检验
                if (!this.localValue) {
                    this.errorInfo = I18n.t('template.方案名称必填');
                } else if (!planNameRule.validator(this.localValue)) {
                    this.errorInfo = planNameRule.message;
                }
                if (this.errorInfo) {
                    return;
                }
                
                this.isSubmiting = true;
                // 重名检测
                TaskPlanService.planCheckName({
                    templateId: this.data.templateId,
                    planId: this.data.id,
                    name: this.localValue,
                }).then((checkResult) => {
                    if (!checkResult) {
                        this.isSubmiting = false;
                        this.errorInfo = I18n.t('template.方案名称已存在，请重新输入');
                        return;
                    }
                    
                    TaskPlanService.planUpdate({
                        id: this.data.id,
                        templateId: this.data.templateId,
                        name: this.localValue,
                        variables: this.data.variableList,
                        enableSteps: this.data.stepList.reduce((result, step) => {
                            if (step.enable) {
                                result.push(step.id);
                            }
                            return result;
                        }, []),
                    }).then(() => {
                        this.localValueMemo = this.localValue;
                        this.isEditing = false;
                        this.$emit('on-edit-success');
                        this.messageSuccess(I18n.t('template.执行方案名称编辑成功'));
                    });
                });
            },
        },
    };
</script>
<style lang="postcss">
    .plan-edit-title {
        position: relative;
        font-size: 14px;

        .input-box {
            position: relative;

            .bk-form-input {
                font-size: 14px;
            }

            &.validate-error {
                .only-bottom-border {
                    border-bottom-color: #ea3636 !important;
                }
            }

            .edit-status-flag {
                position: absolute;
                top: 8px;
                right: 8px;
                z-index: 10;
                font-size: 16px;
            }
        }

        .text {
            font-size: 18px;
            line-height: 32px;
            color: #313238;

            .edit-btn {
                font-size: 15px;
                color: #979ba5;
                cursor: pointer;
            }
        }
    }
</style>
