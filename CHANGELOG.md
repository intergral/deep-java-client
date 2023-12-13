# 1.2.0 (xx/xx/2023)
- **[CHANGE]**: plugin: Change the plugins to use SPI to load [#77](https://github.com/intergral/deep/pull/77) [@Umaaz](https://github.com/Umaaz)

# 1.1.2 (29/09/2023)
- **[CHANGE]**: version: Override grpc-netty dependencies to remove CVE [#44](https://github.com/intergral/deep/pull/44) [@LMarkie](https://github.com/LMarkie)
- **[BUGFIX]**: build: change mvn dep graph to run only on master [#45](https://github.com/intergral/deep/pull/45) [@Umaaz](https://github.com/Umaaz)
- **[BUGFIX]**: build: update build to ensure agent is built for cf-tests [#55](https://github.com/intergral/deep/pull/55) [@Umaaz](https://github.com/Umaaz)
- **[Snyk]**:  Upgrade: net.bytebuddy:byte-buddy-agent from 1.14.4 to 1.14.7 [#43](https://github.com/intergral/deep/pull/43) [@Umaaz](https://github.com/Umaaz)

# 1.1.1 (25/09/2023)
- **[CHANGE]**: reflection: reduce duplicate reflection code and proxy style [#34](https://github.com/intergral/deep/pull/34) [@Umaaz](https://github.com/Umaaz)
- **[Snyk]**: vulnerability: Fix for 1 vulnerability [#35](https://github.com/intergral/deep/pull/35) [@Umaaz](https://github.com/Umaaz)
- **[Snyk]**: Upgrade: org.junit.jupiter:junit-jupiter from 5.9.3 to 5.10.0 [#38](https://github.com/intergral/deep/pull/38) [@Umaaz](https://github.com/Umaaz)

# 1.1.0 (06/09/2023)

- **[CHANGE]**: Make agent start method public [#27](https://github.com/intergral/deep/pull/27) [@Umaaz](https://github.com/Umaaz)
- **[CHANGE]**: move api loader to api module [#16](https://github.com/intergral/deep/pull/16) [@Umaaz](https://github.com/Umaaz)
- **[CHANGE]**: include bytebuddy in deep build [#12](https://github.com/intergral/deep/pull/12) [@Umaaz](https://github.com/Umaaz)
- **[CHANGE]**: add issue and PR templates [#14](https://github.com/intergral/deep/pull/14) [@Umaaz](https://github.com/Umaaz)
- **[CHANGE]**: change docs to use latest release maven site [#14](https://github.com/intergral/deep/pull/14) [@Umaaz](https://github.com/Umaaz)
- **[ENHANCEMENT]**: add unshaded (lite) agent build to maven deploy [#30](https://github.com/intergral/deep/pull/30) [@Umaaz](https://github.com/Umaaz)
- **[ENHANCEMENT]**: add unit tests for agent [#10](https://github.com/intergral/deep/pull/10) [@Umaaz](https://github.com/Umaaz)
- **[ENHANCEMENT]**: Allow plugins to act as resource providers [#23](https://github.com/intergral/deep/pull/23) [@Umaaz](https://github.com/Umaaz)
- **[BUGFIX]**: fix doc generation [#13](https://github.com/intergral/deep/pull/13) [@Umaaz](https://github.com/Umaaz)
- **[BUGFIX]**: fix issue where getShortName would cause class loading [#24](https://github.com/intergral/deep/pull/24) [@Umaaz](https://github.com/Umaaz)

# 1.0.5 (03/08/2023)

- **[ENHANCEMENT]**: allow plugins to act as auth providers [#9](https://github.com/intergral/deep/pull/9) [@Umaaz](https://github.com/Umaaz) 
- **[BUGFIX]**: fix case where jar path is not set [#8](https://github.com/intergral/deep/pull/8) [@Umaaz](https://github.com/Umaaz)

<!-- Template START
# 0.1.1 (16/06/2023)

- **[CHANGE]**: description [#PRid](https://github.com/intergral/deep/pull/8) [@user](https://github.com/)
- **[FEATURE]**: description [#PRid](https://github.com/intergral/deep/pull/) [@user](https://github.com/)
- **[ENHANCEMENT]**: description [#PRid](https://github.com/intergral/deep/pull/) [@user](https://github.com/)
- **[BUGFIX]**: description [#PRid](https://github.com/intergral/deep/pull/) [@user](https://github.com/)
Template END -->
