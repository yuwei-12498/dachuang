const { loadItinerarySnapshot } = require("../store/itinerary")
const { ORIGINAL_FORM_KEY } = require("./storage")

const MAX_ROUTE_NODE_COUNT = 12

function safeString(value) {
  return typeof value === "string" ? value.trim() : ""
}

function numberOrNull(value) {
  const n = Number(value)
  return Number.isFinite(n) ? n : null
}

function intOrNull(value) {
  const n = Number(value)
  return Number.isInteger(n) ? n : null
}

function boolOrDefault(value, fallback = false) {
  if (value === null || value === undefined) {
    return fallback
  }
  return !!value
}

function toStringArray(value) {
  if (!Array.isArray(value)) {
    return []
  }
  return value.map((item) => safeString(item)).filter(Boolean)
}

function resolveActiveOption(snapshot) {
  const options = Array.isArray(snapshot && snapshot.options) ? snapshot.options : []
  if (!options.length) {
    return null
  }
  return options.find((item) => item && item.optionKey === snapshot.selectedOptionKey) || options[0]
}

function normalizeRouteNode(node) {
  if (!node || typeof node !== "object") {
    return null
  }

  const poiName = safeString(node.poiName || node.name)
  if (!poiName) {
    return null
  }

  return {
    poiId: intOrNull(node.poiId || node.id),
    poiName,
    category: safeString(node.category),
    district: safeString(node.district),
    startTime: safeString(node.startTime),
    endTime: safeString(node.endTime),
    travelTime: intOrNull(node.travelTime),
    travelTransportMode: safeString(node.travelTransportMode || node.departureTransportMode),
    travelDistanceKm: numberOrNull(node.travelDistanceKm || node.departureDistanceKm),
    departureTravelTime: intOrNull(node.departureTravelTime),
    departureTransportMode: safeString(node.departureTransportMode),
    departureDistanceKm: numberOrNull(node.departureDistanceKm),
    latitude: numberOrNull(node.latitude),
    longitude: numberOrNull(node.longitude),
    sourceType: safeString(node.sourceType)
  }
}

function extractItineraryContext(snapshot) {
  if (!snapshot || typeof snapshot !== "object") {
    return null
  }

  const activeOption = resolveActiveOption(snapshot)
  const candidateNodes = Array.isArray(activeOption && activeOption.nodes) && activeOption.nodes.length
    ? activeOption.nodes
    : (Array.isArray(snapshot.nodes) ? snapshot.nodes : [])

  const nodes = candidateNodes
    .map(normalizeRouteNode)
    .filter(Boolean)
    .slice(0, MAX_ROUTE_NODE_COUNT)

  if (!nodes.length) {
    return null
  }

  return {
    itineraryId: intOrNull(snapshot.id),
    selectedOptionKey: safeString((activeOption && activeOption.optionKey) || snapshot.selectedOptionKey),
    summary: safeString((activeOption && activeOption.summary) || snapshot.recommendReason),
    totalDuration: intOrNull((activeOption && activeOption.totalDuration) || snapshot.totalDuration),
    totalCost: numberOrNull((activeOption && activeOption.totalCost) || snapshot.totalCost),
    nodes
  }
}

function readOriginalReq() {
  const fromStorage = wx.getStorageSync(ORIGINAL_FORM_KEY)
  if (!fromStorage || typeof fromStorage !== "object") {
    return null
  }
  return fromStorage
}

function resolveBaseForm(currentForm, itinerarySnapshot) {
  const fromStorage = readOriginalReq()
  const fromSnapshot = itinerarySnapshot && itinerarySnapshot.originalReq && typeof itinerarySnapshot.originalReq === "object"
    ? itinerarySnapshot.originalReq
    : null

  return Object.assign({}, fromSnapshot || {}, fromStorage || {}, currentForm || {})
}

function buildSharedChatContext({ pageType = "page", currentForm = null } = {}) {
  const itinerarySnapshot = loadItinerarySnapshot()
  const baseForm = resolveBaseForm(currentForm, itinerarySnapshot)
  const itinerary = extractItineraryContext(itinerarySnapshot)

  return {
    pageType: safeString(pageType) || "page",
    preferences: toStringArray(baseForm.themes),
    rainy: boolOrDefault(baseForm.isRainy, false),
    nightMode: boolOrDefault(baseForm.isNight, false),
    companionType: safeString(baseForm.companionType),
    cityCode: safeString(baseForm.cityCode),
    cityName: safeString(baseForm.cityName),
    userLat: numberOrNull(baseForm.departureLatitude),
    userLng: numberOrNull(baseForm.departureLongitude),
    itinerary,
    recentPois: []
  }
}

module.exports = {
  buildSharedChatContext
}