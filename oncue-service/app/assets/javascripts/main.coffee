console.log "Opening web socket..."
websocket = new WebSocket("ws://localhost:9000/websocket")
websocket.onopen = -> console.log "Web socket open!"
websocket.onmessage = (message) -> console.log message
websocket.onerror = -> console.log "Failed to create web socket :-("