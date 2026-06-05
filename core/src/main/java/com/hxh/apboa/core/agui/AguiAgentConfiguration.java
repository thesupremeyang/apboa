package com.hxh.apboa.core.agui;

import com.hxh.apboa.agent.service.AgentDefinitionService;
import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.core.agent.IAgentFactory;
import io.agentscope.core.agui.registry.AguiAgentRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

/**
 * 描述：AGUI智能体注册
 *
 * @author huxuehao
 **/
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AguiAgentConfiguration implements ApplicationRunner {
    private final AgentDefinitionService agentDefinitionService;
    private final IAgentFactory iAgentFactory;
    private final AguiAgentRegistry registry;

    @Override
    public void run(ApplicationArguments args) {
        configureAgents(registry);
    }
    public void configureAgents(AguiAgentRegistry registry) {
        agentDefinitionService.list()
                .stream()
                .filter(item -> item.getEnabled() == true)
                .filter(item -> item.getAgentCode() != null && !item.getAgentCode().isEmpty())
                .forEach(agentDefinition -> {
                    try {
                        registry.registerFactory(
                                agentDefinition.getAgentCode(),
                                () -> iAgentFactory.getAgent(agentDefinition));
                        log.info("Registered agent: {}", agentDefinition.getAgentCode());
                    } catch (Exception e) {
                        log.error("Failed to register agent {}: {}", agentDefinition.getAgentCode(), e.getMessage());
                    }
                });
    }

    /**
     * 重新注册智能体
     * @param agentDefinition 智能体定义
     */
    public void reRegisterAgent(AgentDefinition agentDefinition) {
        if (registry == null) return;
        if (agentDefinition.getAgentCode() == null || agentDefinition.getAgentCode().isEmpty()) {
            log.warn("Skipping re-register agent with null/empty agentCode, agentId: {}", agentDefinition.getId());
            return;
        }

        try {
            unregisterAgent(agentDefinition.getAgentCode());
            registry.registerFactory(
                    agentDefinition.getAgentCode(),
                    () -> iAgentFactory.getAgent(agentDefinition));
            log.info("Re-registered agent: {}", agentDefinition.getAgentCode());
        } catch (Exception e) {
            log.error("Failed to re-register agent {}: {}", agentDefinition.getAgentCode(), e.getMessage());
        }
    }

    /**
     * 注销单个智能体注册
     *
     * @param agentCode 智能体Code
     */
    public void unregisterAgent(String agentCode) {
        try {
            registry.unregister(agentCode);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
