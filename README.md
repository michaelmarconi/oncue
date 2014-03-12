OnCue job scheduling framework
==============================

[![Build Status](https://travis-ci.org/michaelmarconi/oncue.png)](https://travis-ci.org/michaelmarconi/oncue)

OnCue is a robust, [Akka](http://akka.io/)-based, distributed job scheduling framework.  OnCue comprises a central job scheduling service, [HTTP API](http://docs.oncue.apiary.io/) and Web UI, as well as one or more remote agents.  

When jobs are scheduled at the service, they are farmed out to one of the registered agents, using either predefined scheduling algorithms or ones that **you define yourself**.  This means you can get get **up-and-running immediately** and move to more sophisticated scheduling algorithms later.
