/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.common.k8s.provider;

import com.tencent.bk.job.common.discovery.ServiceInfoProvider;
import com.tencent.bk.job.common.discovery.model.ServiceInstanceInfoDTO;
import com.tencent.bk.job.common.util.json.JsonUtils;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1PodStatus;
import io.kubernetes.client.util.Config;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.kubernetes.commons.discovery.KubernetesServiceInstance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class K8SServiceInfoProvider implements ServiceInfoProvider {

    public final String KEY_HELM_NAMESPACE = "meta.helm.sh/release-namespace";
    public final String KEY_JOB_MS_VERSION = "bk.job.image/tag";
    public final String VERSION_UNKNOWN = "-";
    public final String NAMESPACE_DEFAULT = "default";
    public final String PHASE_RUNNING = "Running";
    private final DiscoveryClient discoveryClient;

    public K8SServiceInfoProvider(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
        log.debug("K8sServiceInfoServiceImpl inited");
    }

    private String getNameSpace(ServiceInstance serviceInstance) {
        KubernetesServiceInstance k8sServiceInstance = (KubernetesServiceInstance) serviceInstance;
        String namespace = k8sServiceInstance.getNamespace();
        if (StringUtils.isNotBlank(namespace)) return namespace;
        return serviceInstance.getMetadata().getOrDefault(KEY_HELM_NAMESPACE, NAMESPACE_DEFAULT);
    }

    private V1Pod findPodByUid(V1PodList podList, String uid) {
        if (podList == null) return null;
        for (V1Pod pod : podList.getItems()) {
            V1ObjectMeta metaData = pod.getMetadata();
            if (metaData != null && uid.equals(metaData.getUid())) {
                return pod;
            }
        }
        return null;
    }

    private Byte convertPodStatus(V1PodStatus podStatus) {
        if (podStatus == null) return ServiceInstanceInfoDTO.STATUS_UNKNOWN;
        String phase = podStatus.getPhase();
        if (StringUtils.isNotBlank(phase)) {
            if (phase.equals(PHASE_RUNNING))
                return ServiceInstanceInfoDTO.STATUS_OK;
            else
                return ServiceInstanceInfoDTO.STATUS_ERROR;
        }
        return ServiceInstanceInfoDTO.STATUS_UNKNOWN;
    }

    private ServiceInstanceInfoDTO getDetailFromK8s(
        ServiceInstance serviceInstance
    ) {
        ServiceInstanceInfoDTO serviceInstanceInfoDTO = new ServiceInstanceInfoDTO();
        serviceInstanceInfoDTO.setServiceName(serviceInstance.getServiceId());
        serviceInstanceInfoDTO.setIp(serviceInstance.getHost());
        serviceInstanceInfoDTO.setPort(serviceInstance.getPort());
        String namespace = getNameSpace(serviceInstance);
        V1PodList podList;
        try {
            ApiClient client = Config.defaultClient();
            Configuration.setDefaultApiClient(client);

            CoreV1Api api = new CoreV1Api();
            podList = api.listNamespacedPod(
                namespace, null, null, null,
                null, null, null, null,
                null, null, null);
        } catch (ApiException | IOException e) {
            log.error("Fail to get pod info from k8s API", e);
            serviceInstanceInfoDTO.setName(serviceInstance.getInstanceId());
            serviceInstanceInfoDTO.setVersion(VERSION_UNKNOWN);
            serviceInstanceInfoDTO.setStatusCode(convertPodStatus(null));
            serviceInstanceInfoDTO.setStatusMessage("Fail to get pod info from k8s");
            return serviceInstanceInfoDTO;
        }
        V1Pod pod = findPodByUid(podList, serviceInstance.getInstanceId());
        V1ObjectMeta metaData = pod.getMetadata();
        if (metaData != null) {
            serviceInstanceInfoDTO.setName(metaData.getName());
        }
        if (metaData != null && metaData.getLabels() != null) {
            String version = metaData.getLabels().getOrDefault(KEY_JOB_MS_VERSION, VERSION_UNKNOWN);
            serviceInstanceInfoDTO.setVersion(version);
            log.debug("namespace={},version={}", namespace, version);
        }
        V1PodStatus podStatus = pod.getStatus();
        if (podStatus != null) {
            serviceInstanceInfoDTO.setStatusCode(convertPodStatus(podStatus));
            serviceInstanceInfoDTO.setStatusMessage(podStatus.getReason());
        }
        if (log.isDebugEnabled()) {
            log.debug("podStatus={}", JsonUtils.toJson(podStatus));
        }
        return serviceInstanceInfoDTO;
    }

    @Override
    public List<ServiceInstanceInfoDTO> listServiceInfo() {
        List<String> serviceIdList = discoveryClient.getServices();
        List<ServiceInstance> serviceInstanceList = new ArrayList<>();
        for (String serviceId : serviceIdList) {
            serviceInstanceList.addAll(discoveryClient.getInstances(serviceId));
        }
        for (ServiceInstance serviceInstance : serviceInstanceList) {
            log.debug("serviceInstance={}", JsonUtils.toJson(serviceInstance));
        }
        return serviceInstanceList.parallelStream().filter(serviceInstance -> {
            if (serviceInstance.getServiceId().contains("job-gateway-management")) {
                return false;
            } else {
                return serviceInstance.getServiceId().contains("job-");
            }
        }).map(this::getDetailFromK8s).collect(Collectors.toList());
    }

}
