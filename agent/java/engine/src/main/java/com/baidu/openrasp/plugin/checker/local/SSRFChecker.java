/*
 * Copyright 2017-2018 Baidu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baidu.openrasp.plugin.checker.local;

import com.baidu.openrasp.config.Config;
import com.baidu.openrasp.plugin.checker.CheckParameter;
import com.baidu.openrasp.plugin.checker.js.JsChecker;
import com.baidu.openrasp.plugin.info.AttackInfo;
import com.baidu.openrasp.plugin.info.EventInfo;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by tyy on 17-12-20.mes
 *
 * SSRF 检测 java 版本
 */
public class SSRFChecker extends ConfigurableChecker {

    private static final String CONFIG_KEY_SSRF_AWS = "ssrf_aws";
    private static final String CONFIG_KEY_SSRF_COMMON = "ssrf_common";
    private static final String CONFIG_KEY_SSRF_OBFUSCATE = "ssrf_obfuscate";
    private static final String CONFIG_KEY_SSRF_INTRANET = "ssrf_intranet";
    private static final String[] INTRANET_DETECTION_SUFFIX = new String[]{".xip.io", ".burpcollaborator.net",
            ".xip.name", ".nip.io", ".vcap.me"};

    @Override
    public List<EventInfo> checkParam(CheckParameter checkParameter) {
        List<EventInfo> result = new LinkedList<EventInfo>();
        String hostName = (String) checkParameter.getParam("hostname");
        JsonObject config = Config.getConfig().getAlgorithmConfig();

        if (!isModuleIgnore(config, CONFIG_KEY_SSRF_INTRANET)) {
            boolean isFound = false;
            for (String suffix : INTRANET_DETECTION_SUFFIX) {
                if (hostName.endsWith(suffix)) {
                    isFound = true;
                    break;
                }
            }
            if (isFound || hostName.equals("requestb.in")) {
                result.add(AttackInfo.createLocalAttackInfo(checkParameter,
                        getActionElement(config, CONFIG_KEY_SSRF_INTRANET), "访问已知的内网探测域名"));
            }
        } else if (!isModuleIgnore(config, CONFIG_KEY_SSRF_AWS)
                && hostName.equals("169.254.169.254")) {
            result.add(AttackInfo.createLocalAttackInfo(checkParameter,
                    getActionElement(config, CONFIG_KEY_SSRF_AWS), "尝试读取 AWS metadata"));
        } else if (!isModuleIgnore(config, CONFIG_KEY_SSRF_COMMON)
                && StringUtils.isNumeric(hostName)) {
            result.add(AttackInfo.createLocalAttackInfo(checkParameter,
                    getActionElement(config, CONFIG_KEY_SSRF_COMMON), "尝试使用纯数字IP"));
        } else if (!isModuleIgnore(config, CONFIG_KEY_SSRF_OBFUSCATE)
                && hostName.startsWith("0x") && !hostName.contains(".")) {
            result.add(AttackInfo.createLocalAttackInfo(checkParameter,
                    getActionElement(config, CONFIG_KEY_SSRF_OBFUSCATE), "尝试使用16进制IP"));
        }

        List<EventInfo> jsResults = new JsChecker().checkParam(checkParameter);
        if (jsResults != null && jsResults.size() > 0) {
            result.addAll(jsResults);
        }
        return result;
    }

    private boolean isModuleIgnore(JsonObject config, String configKey) {
        String action = getActionElement(config, configKey);
        return EventInfo.CHECK_ACTION_IGNORE.equals(action) || action == null;
    }

}
