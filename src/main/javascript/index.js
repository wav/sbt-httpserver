class BuildService {

  constructor(config) {

    /* params */

    var
      // (serviceType: String, state: String, error: ErrorEvent) => void
      onServiceEvent = config.onServiceEvent,

      // (project: String, data: String) => void
      onBuildEvent = config.onBuildEvent,

      // (reply: (m: js) => void, data: js) => reply(data)
      onBuildCommand = config.onBuildCommand,

      // ws://localhost:8083/buildService/events
      eventServiceAddress = config.eventService,

      // ws://localhost:8083/buildService/commands
      commandServiceAddress = config.commandService;

    /* public */

    this.start = () => {
      stopped = false;
      if (tryConnectHandles[BES] === undefined) {
        tryConnectHandles[BES] = setInterval(startEventService,2000);
      }
      if (tryConnectHandles[BCS] === undefined) {
        tryConnectHandles[BCS] = setInterval(startCommandService,2000);
      }
    };

    this.stop = () => {
      stopped = true;
      if (eventService !== undefined) eventService.close();
      if (commandService !== undefined) commandService.close();
    };

    /* private */

    var
      stopped = false,
      tryConnectHandles = {},

      makeService = (id, address, onMessage, close) => {
        if (address === undefined) return;
        var ws = new WebSocket(address);
        ws.onopen = e => {
          clearInterval(tryConnectHandles[id]);
          delete tryConnectHandles[id];
          var handler = onServiceEvent || console.debug;
          handler(id, "Started", undefined);
        };
        ws.onclose = e => {
          close();
          var handler = onServiceEvent || console.debug;
          handler(id, "Closed", undefined);
        };
        ws.onerror = e => {
          var handler = onServiceEvent || console.debug;
          handler(id, "Error", e);
        };
        ws.onmessage = e => {
          var
            m = JSON.parse(e.data),
            handler = onMessage || console.debug;
          handler(m);
        };
        return ws;
      },

      BES = "BuildEventService",
      eventService = undefined,
      startEventService = () => {
        if (eventService !== undefined) return;
        eventService = makeService(
          BES,
          eventServiceAddress,
          e => {
            var
              [project, event] = e,
              handler = onBuildEvent || console.debug;

            if (project === undefined || event === undefined) return;
            handler(project,event);
          },
          () => {
            eventService = undefined;
            if (!stopped) this.start();
          });
      },

      BCS = "BuildCommandService",
      commandService = undefined,
      startCommandService = () => {
        if (commandService !== undefined) return;
        commandService = makeService(
          BCS,
          commandServiceAddress,
          e => {
            var
              [id, data] = e,
              reply = m => commandService.send(JSON.stringify([id, m])),
              handler = onBuildCommand || console.debug;
            handler(reply, data);
          },
          () => {
            commandService = undefined;
            if (!stopped) this.start();
          });
      };

  }

}