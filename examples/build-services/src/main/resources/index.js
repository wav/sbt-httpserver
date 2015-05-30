var BuildServices = (function() {

  var
      commandService = null,
      buildService = null,
      tryConnectHandle = null,
      started = null,
      stopped = null,
      handlers = {
        "echo": send
      };

    function start(onstart,onstop) {
      started = onstart;
      stopped = onstop;
      startCommandService();
      startBuildService();
    }

    function opened(id) {
      started(id);
      if (buildService != null && commandService != null) return;
      clearInterval(tryConnectHandle);
      tryConnectHandle = null;
    }

    function closed(id) {
      if (tryConnectHandle != null) return;
      tryConnectHandle = setInterval(function() { start(started, stopped); },2000);
      stopped(id);
    }

    function startCommandService() {
      if (commandService != null) return;
      var ws = new WebSocket("ws://localhost:8083/commands");
      ws.onopen = function(e) {
        opened("commandService");
      }
      ws.onclose = function(e) {
        commandService = null;
        closed("commandService");
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
      commandService = ws;
    }

    function startBuildService() {
      if (buildService != null) return;
      var ws = new WebSocket("ws://localhost:8083/buildEvents");
      ws.onopen = function(e) {
        opened("buildService");
      }
      ws.onclose = function(e) {
        buildService = null;
        closed("buildService");
      }
      ws.onerror = console.error;
      ws.onmessage = function(e) {
        console.debug("reload");
        location.reload();
      };
      buildService = ws;
    }

    function stop() {
      if (tryConnectHandle != null) {
        clearInterval(tryConnectHandle);
        tryConnectHandle = null;
      }
      if (commandService != null) {
        var svc = commandService;
        commandService = null;
        svc.close();
      }
      if (buildService != null) {
        var svc = buildService;
        buildService = null;
        svc.close();
      }
    }

    function send(command, data) {
      if (commandService == null) return;
      var m = {};
      m[command]=data;
      console.debug("sent", m);
      commandService.send(JSON.stringify(m));
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