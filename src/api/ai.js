import request from '@/utils/request'

/**
 * AI主观题判分
 * @param {Object} data - 判分参数
 * @param {string} data.question - 题目内容
 * @param {string} data.standardAnswer - 标准答案
 * @param {string} data.studentAnswer - 学生答案
 * @param {number} data.fullScore - 满分
 */
export function aiGrade(data) {
  return request({
    url: '/api/ai/grade',
    method: 'post',
    data
  })
}

/**
 * AI智能出题
 * @param {Object} data - 出题参数
 * @param {string} data.subject - 科目名称
 * @param {string} data.knowledge - 知识点
 * @param {number} data.type - 题型 (1单选/2多选/3判断/4主观)
 * @param {number} data.difficulty - 难度 (1-5)
 * @param {number} data.count - 数量
 */
export function aiGenerateQuestions(data) {
  return request({
    url: '/api/ai/generate-questions',
    method: 'post',
    data
  })
}

/**
 * 保存单个AI生成的题目到题库
 * @param {Object} data - 题目信息
 */
export function aiSaveQuestion(data) {
  return request({
    url: '/api/ai/save-question',
    method: 'post',
    data
  })
}

/**
 * 批量保存AI生成的题目到题库
 * @param {Object} data - 包含questions列表、subjectName、type
 */
export function aiSaveQuestions(data) {
  return request({
    url: '/api/ai/save-questions',
    method: 'post',
    data
  })
}
