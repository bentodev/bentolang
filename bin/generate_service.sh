#!/usr/bin/env bash
#
# Generate systemd service file for Bento and write it to
# the standard location for service files

if [ -z "$SERVICE_NAME" ]
then
    SERVICE_NAME=bento
fi

if [ -z "$BENTO_HOME" ]
then
    BENTO_HOME=/opt/$SERVICE_NAME
fi

cat > /usr/lib/systemd/system/$SERVICE_NAME.service <<EOF
[Unit]
Description=BentoServer
After=syslog.target
After=network.target

[Service]
Type=simple
ExecStart=$BENTO_HOME/bin/bentoserver.sh start
ExecStop=$BENTO_HOME/bin/bentoserver.sh stop
ExecReload=$BENTO_HOME/bin/bentoserver.sh restart
Restart=on-success
User=bento
Group=bento
TimeoutSec=300

[Install]
WantedBy=multi-user.target
EOF
