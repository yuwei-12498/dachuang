function formatDuration(minutes) {
  if (minutes !== 0 && !minutes) {
    return "--"
  }
  const hour = Math.floor(minutes / 60)
  const minute = minutes % 60
  if (hour === 0) {
    return `${minute}分钟`
  }
  if (minute === 0) {
    return `${hour}小时`
  }
  return `${hour}小时 ${minute}分钟`
}

function formatDateTime(value) {
  if (!value) {
    return "--"
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return "--"
  }
  const month = String(date.getMonth() + 1).padStart(2, "0")
  const day = String(date.getDate()).padStart(2, "0")
  const hour = String(date.getHours()).padStart(2, "0")
  const minute = String(date.getMinutes()).padStart(2, "0")
  return `${month}-${day} ${hour}:${minute}`
}

module.exports = {
  formatDuration,
  formatDateTime
}
