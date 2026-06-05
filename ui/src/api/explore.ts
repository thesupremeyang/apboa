/**
 * Agent广场 - API接口
 *
 * @author huxuehao
 */
import request from '@/utils/request'

/**
 * Agent数据类型
 */
export interface ExploreAgent {
  id: string
  name: string
  description: string
  agentType: 'CUSTOM' | 'A2A'
  tag?: string
  toolCount?: number
  skillCount?: number
  knowledgeCount?: number
  mcpCount?: number
  updatedAt?: string
}

/**
 * 分页查询参数
 */
export interface ExplorePageParams {
  page: number
  size: number
  keyword?: string
  tag?: string
  agentType?: string
}

/**
 * 分页查询Agent列表
 */
export function page(params: ExplorePageParams) {
  return request.get('/agent/definition/page', { params })
}

/**
 * 获取Agent详情
 */
export function detail(id: string) {
  return request.get(`/agent/definition/${id}`)
}

/**
 * 获取所有标签
 */
export function getTags() {
  return request.get('/agent/definition/get/tags')
}

/**
 * 获取Agent启用的工具
 */
export function getEnabledTools(agentId: string) {
  return request.get(`/agent/definition/${agentId}/enabled/tools`)
}

/**
 * 获取Agent启用的技能包
 */
export function getEnabledSkills(agentId: string) {
  return request.get(`/agent/definition/${agentId}/enabled/skills`)
}
