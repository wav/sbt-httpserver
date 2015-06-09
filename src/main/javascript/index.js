class BuildService {

  constructor(config) {

    var
      BES = "BuildEventService",
      BCS = "BuildCommandService",
      addresses = {};

    /* params */

    var
      // (serviceType: String, state: String, error: ErrorEvent) => void
      onServiceEvent = config.onServiceEvent,

      // (project: String, data: String) => void
      onBuildEvent = config.onBuildEvent,

      // (reply: (m: js) => void, data: js) => reply(data)
      onBuildCommand = config.onBuildCommand;

    // ws://localhost:8083/buildService/events
    addresses[BES] = config.eventService;

    // ws://localhost:8083/buildService/commands
    addresses[BCS] = config.commandService;

    /* public */

    this.start = () => {
      stopped = false;
      for (var id in addresses) startConnecting(id);
    };

    this.stop = () => {
      stopped = true;
      for (var id in addresses) {
        stopConnecting(id);
        if (connections[id] !== undefined) connections[id].close();
      }
    };

    /* private */

    var
      stopped = false,
      handlers = {},
      tryConnectHandles = {},
      connections = {},
      debugServiceEvent = (serviceType, state, error) => console.debug("onServiceEvent", serviceType, state, error),
      debugMessage = m => console.debug("onMessage", m),
      startConnecting = id => {
        var start = () => {
          if (!stopped && tryConnectHandles[id] === undefined);
          tryStart(id);
        }
        start();
        tryConnectHandles[id] = setInterval(start,2000);
      },
      stopConnecting = id => {
        if (tryConnectHandles[id] !== undefined) {
          clearInterval(tryConnectHandles[id]);
          delete tryConnectHandles[id];
        }
      },
      tryStart = id => {
        var ws = new WebSocket(addresses[id]);
        ws.onopen = e => {
          stopConnecting(id);
          var handler = onServiceEvent || debugServiceEvent;
          handler(id, "Started", undefined);
        };
        ws.onclose = e => {
          if (connections[id] !== undefined) delete connections[id];
          if (tryConnectHandles[id] !== undefined) return;
          var handler = onServiceEvent || debugServiceEvent;
          handler(id, "Closed", undefined);
          startConnecting(id);
        };
        ws.onerror = e => {
          var handler = onServiceEvent || debugServiceEvent;
          handler(id, "Error", e);
        };
        ws.onmessage = e => {
          var
            m = JSON.parse(e.data),
            handler = handlers[id] || debugMessage;
          handler(m);
        };
        connections[id] = ws;
      };

    var debugBuildEvent = (project, event) => console.debug("onBuildEvent", project, event);
    handlers[BES] = e => {
      var
        [project, event] = e,
        handler = onBuildEvent || debugBuildEvent;
      if (project === undefined || event === undefined) return;
      handler(project,event);
    };

    var debugBuildCommand = (reply, data) => console.debug("onBuildCommand", data);
    handlers[BCS] = e => {
      var
        [id, data] = e,
        reply = m => commandService.send(JSON.stringify([id, m])),
        handler = onBuildCommand || debugBuildCommand;
      handler(reply, data);
    };

  }

}