
npm install -g webidl-extract
curl -O https://raw.githubusercontent.com/w3c/webrtc-pc/master/archives/20170313/webrtc.html
cat webrtc.html | webidl-extract > output.idl
