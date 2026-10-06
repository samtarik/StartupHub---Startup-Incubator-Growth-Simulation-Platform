/* STOMP-over-SockJS client. Subscribes to this user's private notification
   channel, which the Observer pattern on the server publishes to whenever an
   investment lands or a milestone is completed.

   sockjs.min.js and stomp.umd.min.js are served from /js/vendor, so live
   notifications keep working with no internet connection. */
function connectNotifications(userId, onNotification) {
  if (typeof SockJS === 'undefined' || typeof StompJs === 'undefined') {
    setWsIndicator(false);
    return null;
  }

  const client = new StompJs.Client({
    webSocketFactory: () => new SockJS('/ws'),
    reconnectDelay: 4000,
    onConnect: () => {
      setWsIndicator(true);
      client.subscribe(`/topic/notifications-${userId}`, (frame) => {
        let notif;
        try { notif = JSON.parse(frame.body); } catch (_) { return; }
        toast(notif.message, 'success');
        refreshBell();
        if (onNotification) onNotification(notif);
      });
    },
    onWebSocketClose: () => setWsIndicator(false),
    onStompError: () => setWsIndicator(false),
  });

  client.activate();
  return client;
}
