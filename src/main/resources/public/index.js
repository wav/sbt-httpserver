var BuildServices = (function() {

  var
      buildCommandService = null,
      buildEventService = null,
      tryConnectHandle = null,
      started = null,
      stopped = null,
      handlers = {
        "echo": send
      };

     function onBuildEvent(project, event) {
        console.debug("reload");
        location.reload();
     }

    function start(onstart,onstop) {
      started = onstart;
      stopped = onstop;
      startBuildCommandService();
      startBuildEventService();
    }

    function opened(id) {
      started(id);
      if (buildEventService != null && buildCommandService != null) return;
      clearInterval(tryConnectHandle);
      tryConnectHandle = null;
    }

    function closed(id) {
      if (tryConnectHandle != null) return;
      tryConnectHandle = setInterval(function() { start(started, stopped); },2000);
      stopped(id);
    }

    function startBuildCommandService() {
      if (buildCommandService != null) return;
      var ws = new WebSocket(BuildServicesConfig.buildCommandService);
      ws.onopen = function(e) {
        opened("buildCommandService");
      }
      ws.onclose = function(e) {
        buildCommandService = null;
        closed("buildCommandService");
      }
      ws.onerror = console.error;
      ws.onmessage = function(e) {
        var
          m = JSON.parse(e.data),
          command = Object.keys(m)[0],
          handler = handlers[command];
        console.debug("received", m);
        handler(command, m[command]);
      };
      buildCommandService = ws;
    }

    function startBuildEventService() {
      if (buildEventService != null) return;
      var ws = new WebSocket(BuildServicesConfig.buildEventService);
      ws.onopen = function(e) {
        opened("buildEventService");
      }
      ws.onclose = function(e) {
        buildEventService = null;
        closed("buildEventService");
      }
      ws.onerror = console.error;
      ws.onmessage = function(e) {
        var m = JSON.parse(e.data);
        onBuildEvent(m["project"], m["event"]);
      };
      buildEventService = ws;
    }

    function stop() {
      if (tryConnectHandle != null) {
        clearInterval(tryConnectHandle);
        tryConnectHandle = null;
      }
      if (buildCommandService != null) {
        var svc = buildCommandService;
        buildCommandService = null;
        svc.close();
      }
      if (buildEventService != null) {
        var svc = buildEventService;
        buildEventService = null;
        svc.close();
      }
    }

    function send(command, data) {
      if (buildCommandService == null) return;
      var m = {};
      m[command]=data;
      console.debug("sent", m);
      buildCommandService.send(JSON.stringify(m));
    }

  return {
    "start" : function () {
      start(
        function(id) {
            console.debug("Started " + id);
        },
        function(id) {
            console.debug("Stopped " + id);
        });
    },
    "stop" : stop,
    "on" : function(command, handler) {
      handlers[command] = handler;
    },
    "send" : send,
  };
})();