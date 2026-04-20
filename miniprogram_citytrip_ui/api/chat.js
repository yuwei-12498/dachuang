const request = require("../utils/request")

function reqAskChat(data) {
  return request({
    url: "/api/chat/messages",
    method: "POST",
    data
  })
}

function reqGetChatStatus() {
  return request({
    url: "/api/chat/messages/status",
    method: "GET",
    skipErrorMessage: true
  })
}

module.exports = {
  reqAskChat,
  reqGetChatStatus
}
