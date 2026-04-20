const {
  ITINERARY_KEY,
  ITINERARY_SESSION_KEY,
  ORIGINAL_FORM_KEY
} = require("../utils/storage")

const WEEKDAY_MAP = {
  Monday: "周一",
  Tuesday: "周二",
  Wednesday: "周三",
  Thursday: "周四",
  Friday: "周五",
  Saturday: "周六",
  Sunday: "周日"
}

function getDefaultTripDate() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, "0")
  const day = String(now.getDate()).padStart(2, "0")
  return `${year}-${month}-${day}`
}

function localizeItineraryText(value) {
  if (typeof value !== "string" || !value.trim()) {
    return value
  }

  let text = value.trim()

  const exactMap = {
    "A more efficient route has been generated.": "已为你生成一版更顺路的路线。",
    "Current route is already close to optimal.": "当前路线已经接近最优。",
    "No valid route found for the current time window.": "在当前时间窗口内没有找到可执行的路线。",
    "Route reordered using travel time and opening-hour constraints.": "已根据交通顺序和营业时间重新整理路线。",
    "Route recalculated after replacing the selected poi.": "已根据新点位重算路线。",
    "No executable route was found.": "没有找到可执行的路线。",
    "Try a wider time window or a different trip date.": "请尝试放宽时间窗口或更换出行日期。",
    "Closed or unavailable venues were filtered out.": "系统已优先过滤闭馆或暂不可用的场馆。",
    "Business hours need confirmation.": "营业时间信息不完整，请出发前再次确认。",
    "Status is stale and should be rechecked.": "当前场馆状态超过 14 天未核验，请出发前再次确认。",
    "Temporarily closed.": "景点当前处于临时关闭状态。",
    "No itinerary to replan.": "当前没有可重排的行程。",
    "Route updated.": "已更新为新的路线。",
    "Route kept": "保留当前路线",
    "Replan failed.": "重排失败，请稍后重试。"
  }

  if (exactMap[text]) {
    return exactMap[text]
  }

  text = text.replace(/^Trip date:\s*([0-9-]+)/, "出行日期：$1")
  text = text.replace(
    /^The route starts at (.+) and finishes at (.+), prioritizing stronger POIs with lower backtracking cost\.$/,
    "路线从 $1 开始，在 $2 收束，优先串联了更高分且更顺路的点位。"
  )
  text = text.replace(/^theme match:\s*/i, "主题匹配：")
  text = text.replace(/\bfits companion type\b/gi, "适合同伴类型")
  text = text.replace(/\bbetter score and lower detour cost\b/gi, "综合得分更高且折返成本更低")
  text = text.replace(/Business hours need confirmation\./g, "营业时间信息不完整，请出发前再次确认。")
  text = text.replace(/Status is stale and should be rechecked\./g, "当前场馆状态超过 14 天未核验，请出发前再次确认。")
  text = text.replace(/Temporarily closed\./g, "景点当前处于临时关闭状态。")
  text = text.replace(/Wait about (\d+) minutes for opening time\./g, "到达后需等待约 $1 分钟开门。")
  text = text.replace(/Closed on ([A-Za-z]+)\./g, (_, day) => `该景点在${WEEKDAY_MAP[day] || day}不开放。`)
  text = text.replace(/Replaced (.+) with a better nearby alternative\./g, "已将 $1 替换为更适合当前路线的相近点位。")
  text = text.replace(/The best feasible ordering did not change under the current constraints\./g, "在当前约束下，最优组合和顺序基本没有变化。")
  text = text.replace(/The new route reduces backtracking and rechecks venue availability\./g, "新路线降低了折返成本，并重新校验了场馆状态。")
  text = text.replace(/No better route was found under the current constraints\./g, "在当前条件下没有更优路线了。")
  text = text.replace(/No itinerary snapshot is available\. Generate or restore one first\./g, "没有可用的行程快照，请先生成或恢复行程。")
  text = text.replace(/Failed to load poi detail\./g, "加载点位详情失败。")
  text = text.replace(/Stop replaced and route refreshed\./g, "点位已替换，行程也同步刷新了。")
  text = text.replace(/;\s*/g, "；")

  return text
}

function normalizePoiDetail(detail) {
  if (!detail || typeof detail !== "object") {
    return detail
  }

  return Object.assign({}, detail, {
    availabilityNote: localizeItineraryText(detail.availabilityNote),
    statusNote: localizeItineraryText(detail.statusNote),
    description: localizeItineraryText(detail.description)
  })
}

function normalizeItinerarySnapshot(snapshot) {
  if (!snapshot || typeof snapshot !== "object") {
    return snapshot
  }

  const normalized = Object.assign({}, snapshot, {
    recommendReason: localizeItineraryText(snapshot.recommendReason),
    tips: localizeItineraryText(snapshot.tips),
    alerts: Array.isArray(snapshot.alerts)
      ? snapshot.alerts.map(localizeItineraryText)
      : snapshot.alerts,
    nodes: Array.isArray(snapshot.nodes)
      ? snapshot.nodes.map((node) =>
          Object.assign({}, node, {
            sysReason: localizeItineraryText(node.sysReason),
            statusNote: localizeItineraryText(node.statusNote)
          })
        )
      : snapshot.nodes
  })

  if (Array.isArray(snapshot.options)) {
    normalized.options = snapshot.options.map((option) =>
      Object.assign({}, option, {
        summary: localizeItineraryText(option.summary),
        recommendReason: localizeItineraryText(option.recommendReason),
        notRecommendReason: localizeItineraryText(option.notRecommendReason),
        alerts: Array.isArray(option.alerts) ? option.alerts.map(localizeItineraryText) : option.alerts,
        nodes: Array.isArray(option.nodes)
          ? option.nodes.map((node) =>
              Object.assign({}, node, {
                sysReason: localizeItineraryText(node.sysReason),
                statusNote: localizeItineraryText(node.statusNote)
              })
            )
          : option.nodes
      })
    )
  }

  return normalized
}

function saveItinerarySnapshot(snapshot) {
  if (!snapshot) {
    return
  }

  const normalized = normalizeItinerarySnapshot(snapshot)
  wx.setStorageSync(ITINERARY_KEY, normalized)
  wx.setStorageSync(ITINERARY_SESSION_KEY, normalized)
  if (normalized.originalReq) {
    wx.setStorageSync(ORIGINAL_FORM_KEY, normalized.originalReq)
  }
}

function loadItinerarySnapshot() {
  const main = wx.getStorageSync(ITINERARY_KEY)
  if (main) {
    const normalized = normalizeItinerarySnapshot(main)
    saveItinerarySnapshot(normalized)
    return normalized
  }

  const session = wx.getStorageSync(ITINERARY_SESSION_KEY)
  if (!session) {
    return null
  }

  const itinerary = normalizeItinerarySnapshot(session)
  const req = wx.getStorageSync(ORIGINAL_FORM_KEY)
  if (req && !itinerary.originalReq) {
    itinerary.originalReq = req
  }
  saveItinerarySnapshot(itinerary)
  return itinerary
}

function clearItinerarySnapshot() {
  wx.removeStorageSync(ITINERARY_KEY)
  wx.removeStorageSync(ITINERARY_SESSION_KEY)
  wx.removeStorageSync(ORIGINAL_FORM_KEY)
}

module.exports = {
  getDefaultTripDate,
  localizeItineraryText,
  normalizePoiDetail,
  normalizeItinerarySnapshot,
  saveItinerarySnapshot,
  loadItinerarySnapshot,
  clearItinerarySnapshot
}
