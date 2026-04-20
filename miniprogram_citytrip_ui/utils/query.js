function buildQuery(params) {
  if (!params || typeof params !== "object") {
    return ""
  }

  const entries = Object.keys(params)
    .filter((key) => params[key] !== undefined && params[key] !== null && params[key] !== "")
    .map((key) => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)

  return entries.length ? `?${entries.join("&")}` : ""
}

module.exports = {
  buildQuery
}
