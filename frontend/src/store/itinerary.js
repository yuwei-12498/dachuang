const STORAGE_KEY = 'citytrip/current-itinerary-v2'

const WEEKDAY_MAP = {
  Monday: '\u5468\u4E00',
  Tuesday: '\u5468\u4E8C',
  Wednesday: '\u5468\u4E09',
  Thursday: '\u5468\u56DB',
  Friday: '\u5468\u4E94',
  Saturday: '\u5468\u516D',
  Sunday: '\u5468\u65E5'
}

function canUseStorage(type) {
  return typeof window !== 'undefined' && typeof window[type] !== 'undefined'
}

export function getDefaultTripDate() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function localizeItineraryText(value) {
  if (typeof value !== 'string' || !value.trim()) {
    return value
  }

  let text = value.trim()

  const exactMap = {
    'A more efficient route has been generated.': '\u5DF2\u4E3A\u4F60\u751F\u6210\u4E00\u7248\u66F4\u987A\u8DEF\u7684\u8DEF\u7EBF\u3002',
    'Current route is already close to optimal.': '\u5F53\u524D\u8DEF\u7EBF\u5DF2\u7ECF\u63A5\u8FD1\u6700\u4F18\u3002',
    'No valid route found for the current time window.': '\u5728\u5F53\u524D\u65F6\u95F4\u7A97\u53E3\u5185\u6CA1\u6709\u627E\u5230\u53EF\u6267\u884C\u7684\u8DEF\u7EBF\u3002',
    'Route reordered using travel time and opening-hour constraints.': '\u5DF2\u6839\u636E\u4EA4\u901A\u987A\u5E8F\u548C\u8425\u4E1A\u65F6\u95F4\u91CD\u65B0\u6574\u7406\u8DEF\u7EBF\u3002',
    'Route recalculated after replacing the selected poi.': '\u5DF2\u6839\u636E\u65B0\u70B9\u4F4D\u91CD\u7B97\u8DEF\u7EBF\u3002',
    'No executable route was found.': '\u6CA1\u6709\u627E\u5230\u53EF\u6267\u884C\u7684\u8DEF\u7EBF\u3002',
    'Try a wider time window or a different trip date.': '\u8BF7\u5C1D\u8BD5\u653E\u5BBD\u65F6\u95F4\u7A97\u53E3\u6216\u66F4\u6362\u51FA\u884C\u65E5\u671F\u3002',
    'Closed or unavailable venues were filtered out.': '\u7CFB\u7EDF\u5DF2\u4F18\u5148\u8FC7\u6EE4\u95ED\u9986\u6216\u6682\u4E0D\u53EF\u7528\u7684\u573A\u9986\u3002',
    'Business hours need confirmation.': '\u8425\u4E1A\u65F6\u95F4\u4FE1\u606F\u4E0D\u5B8C\u6574\uFF0C\u8BF7\u51FA\u53D1\u524D\u518D\u6B21\u786E\u8BA4\u3002',
    'Status is stale and should be rechecked.': '\u5F53\u524D\u573A\u9986\u72B6\u6001\u8D85\u8FC7 14 \u5929\u672A\u6838\u9A8C\uFF0C\u8BF7\u51FA\u53D1\u524D\u518D\u6B21\u786E\u8BA4\u3002',
    'Temporarily closed.': '\u666F\u70B9\u5F53\u524D\u5904\u4E8E\u4E34\u65F6\u5173\u95ED\u72B6\u6001\u3002',
    'No itinerary to replan.': '\u5F53\u524D\u6CA1\u6709\u53EF\u91CD\u6392\u7684\u884C\u7A0B\u3002',
    'Route updated.': '\u5DF2\u66F4\u65B0\u4E3A\u65B0\u7684\u8DEF\u7EBF\u3002',
    'Route kept': '\u4FDD\u7559\u5F53\u524D\u8DEF\u7EBF',
    'Replan failed.': '\u91CD\u6392\u5931\u8D25\uFF0C\u8BF7\u7A0D\u540E\u91CD\u8BD5\u3002'
  }

  if (exactMap[text]) {
    return exactMap[text]
  }

  text = text.replace(/^Trip date:\s*([0-9-]+)/, '\u51FA\u884C\u65E5\u671F\uFF1A$1')
  text = text.replace(
    /^The route starts at (.+) and finishes at (.+), prioritizing stronger POIs with lower backtracking cost\.$/,
    '\u8DEF\u7EBF\u4ECE $1 \u5F00\u59CB\uFF0C\u5728 $2 \u6536\u675F\uFF0C\u4F18\u5148\u4E32\u8054\u4E86\u66F4\u9AD8\u5206\u4E14\u66F4\u987A\u8DEF\u7684\u70B9\u4F4D\u3002'
  )
  text = text.replace(/^theme match:\s*/i, '\u4E3B\u9898\u5339\u914D\uFF1A')
  text = text.replace(/\bfits companion type\b/gi, '\u9002\u5408\u540C\u884C\u7C7B\u578B')
  text = text.replace(/\bbetter score and lower detour cost\b/gi, '\u7EFC\u5408\u5F97\u5206\u66F4\u9AD8\u4E14\u6298\u8FD4\u6210\u672C\u66F4\u4F4E')
  text = text.replace(/Closed or unavailable venues were filtered out\./g, '\u7CFB\u7EDF\u5DF2\u4F18\u5148\u8FC7\u6EE4\u95ED\u9986\u6216\u6682\u4E0D\u53EF\u7528\u7684\u573A\u9986\u3002')
  text = text.replace(/Business hours need confirmation\./g, '\u8425\u4E1A\u65F6\u95F4\u4FE1\u606F\u4E0D\u5B8C\u6574\uFF0C\u8BF7\u51FA\u53D1\u524D\u518D\u6B21\u786E\u8BA4\u3002')
  text = text.replace(/Status is stale and should be rechecked\./g, '\u5F53\u524D\u573A\u9986\u72B6\u6001\u8D85\u8FC7 14 \u5929\u672A\u6838\u9A8C\uFF0C\u8BF7\u51FA\u53D1\u524D\u518D\u6B21\u786E\u8BA4\u3002')
  text = text.replace(/Temporarily closed\./g, '\u666F\u70B9\u5F53\u524D\u5904\u4E8E\u4E34\u65F6\u5173\u95ED\u72B6\u6001\u3002')
  text = text.replace(/Wait about (\d+) minutes for opening time\./g, '\u5230\u8FBE\u540E\u9700\u7B49\u5F85\u7EA6 $1 \u5206\u949F\u5F00\u95E8\u3002')
  text = text.replace(/Closed on ([A-Za-z]+)\./g, (_, day) => `\u8BE5\u666F\u70B9\u5728${WEEKDAY_MAP[day] || day}\u4E0D\u5F00\u653E\u3002`)
  text = text.replace(/Replaced (.+) with a better nearby alternative\./g, '\u5DF2\u5C06 $1 \u66FF\u6362\u4E3A\u66F4\u5408\u9002\u5F53\u524D\u8DEF\u7EBF\u7684\u76F8\u8FD1\u70B9\u4F4D\u3002')
  text = text.replace(/The best feasible ordering did not change under the current constraints\./g, '\u5728\u5F53\u524D\u7EA6\u675F\u4E0B\uFF0C\u6700\u4F18\u7684\u7EC4\u5408\u548C\u987A\u5E8F\u57FA\u672C\u6CA1\u6709\u53D8\u5316\u3002')
  text = text.replace(/The new route reduces backtracking and rechecks venue availability\./g, '\u65B0\u8DEF\u7EBF\u964D\u4F4E\u4E86\u6298\u8FD4\u6210\u672C\uFF0C\u5E76\u91CD\u65B0\u6821\u9A8C\u4E86\u573A\u9986\u72B6\u6001\u3002')
  text = text.replace(/No better route was found under the current constraints\./g, '\u5728\u5F53\u524D\u6761\u4EF6\u4E0B\u6CA1\u6709\u66F4\u597D\u7684\u66FF\u4EE3\u65B9\u6848\u3002')
  text = text.replace(/No itinerary snapshot is available\. Generate or restore one first\./g, '\u6CA1\u6709\u53EF\u7528\u7684\u884C\u7A0B\u5FEB\u7167\uFF0C\u8BF7\u5148\u751F\u6210\u6216\u6062\u590D\u884C\u7A0B\u3002')
  text = text.replace(/Failed to load poi detail\./g, '\u52A0\u8F7D\u70B9\u4F4D\u8BE6\u60C5\u5931\u8D25\u3002')
  text = text.replace(/Stop replaced and route refreshed\./g, '\u70B9\u4F4D\u5DF2\u66FF\u6362\uFF0C\u884C\u7A0B\u4E5F\u540C\u6B65\u5237\u65B0\u4E86\u3002')
  text = text.replace(/;\s*/g, '\uFF1B')

  return text
}

export function normalizePoiDetail(detail) {
  if (!detail || typeof detail !== 'object') {
    return detail
  }

  return {
    ...detail,
    availabilityNote: localizeItineraryText(detail.availabilityNote),
    statusNote: localizeItineraryText(detail.statusNote),
    description: localizeItineraryText(detail.description)
  }
}

export function normalizeItinerarySnapshot(snapshot) {
  if (!snapshot || typeof snapshot !== 'object') {
    return snapshot
  }

  return {
    ...snapshot,
    recommendReason: localizeItineraryText(snapshot.recommendReason),
    tips: localizeItineraryText(snapshot.tips),
    alerts: Array.isArray(snapshot.alerts) ? snapshot.alerts.map(localizeItineraryText) : snapshot.alerts,
    nodes: Array.isArray(snapshot.nodes)
      ? snapshot.nodes.map(node => ({
          ...node,
          sysReason: localizeItineraryText(node.sysReason),
          statusNote: localizeItineraryText(node.statusNote)
        }))
      : snapshot.nodes
  }
}

export function saveItinerarySnapshot(snapshot) {
  if (!snapshot) {
    return
  }

  const normalized = normalizeItinerarySnapshot(snapshot)

  if (canUseStorage('localStorage')) {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(normalized))
  }
  if (canUseStorage('sessionStorage')) {
    window.sessionStorage.setItem('current_itinerary', JSON.stringify(normalized))
    if (normalized.originalReq) {
      window.sessionStorage.setItem('original_req_form', JSON.stringify(normalized.originalReq))
    }
  }
}

export function loadItinerarySnapshot() {
  if (canUseStorage('localStorage')) {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    if (raw) {
      try {
        const parsed = normalizeItinerarySnapshot(JSON.parse(raw))
        saveItinerarySnapshot(parsed)
        return parsed
      } catch (err) {
        window.localStorage.removeItem(STORAGE_KEY)
      }
    }
  }

  if (canUseStorage('sessionStorage')) {
    const itineraryRaw = window.sessionStorage.getItem('current_itinerary')
    if (!itineraryRaw) {
      return null
    }
    try {
      const itinerary = normalizeItinerarySnapshot(JSON.parse(itineraryRaw))
      const reqRaw = window.sessionStorage.getItem('original_req_form')
      if (reqRaw && !itinerary.originalReq) {
        itinerary.originalReq = JSON.parse(reqRaw)
      }
      saveItinerarySnapshot(itinerary)
      return itinerary
    } catch (err) {
      return null
    }
  }

  return null
}

export function clearItinerarySnapshot() {
  if (canUseStorage('localStorage')) {
    window.localStorage.removeItem(STORAGE_KEY)
  }
  if (canUseStorage('sessionStorage')) {
    window.sessionStorage.removeItem('current_itinerary')
    window.sessionStorage.removeItem('original_req_form')
  }
}
