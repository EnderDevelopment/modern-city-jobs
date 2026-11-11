# Minecraft Jobs and Emerald Wallet Plugin

ModernCityJobs is a configurable Minecraft jobs plugin with job-selection menus, timed shifts, task progress, an internal Emerald Wallet, and Citizens NPC right-click support. Players choose from six configured jobs, complete activities during a shift, and receive configured Emerald rewards in their wallet when they finish the required tasks.

## Install

1. Use a Paper server running Java 21. The Maven project references Paper API `1.21.11-R0.1-SNAPSHOT`; the supplied files do not establish compatibility with Paper 1.21.1.
2. Build the plugin JAR from the project directory with Maven: `mvn package`.
3. Copy the resulting `target/plugin.jar` into your server’s `plugins` folder.
4. Start the server once to generate `plugins/ModernCityJobs/config.yml`, then stop the server to edit it if needed.
5. Restart the server to load the plugin and configuration. Citizens is optional; install it separately if you want NPC right-clicks to open the job center.

## Use

- Run `/jobs` to open the job menu. Select a job, choose a configured shift duration, and confirm to start.
- During a shift, right-click, break a block, or catch a fish to count job activity. The required number of actions is set for each duration in `config.yml`; activity is shared across the configured jobs.
- Complete the required actions before the shift timer expires to receive the configured reward in your wallet. Leaving, dying, or running out of time cancels the shift without a reward.
- Click the wallet item in a job menu to see your balance in chat. Rewards are stored in `plugins/ModernCityJobs/wallets.yml`; they are not physical Emerald items.
- Administrators with `moderncityjobs.admin` can use `/jobadmin check <player>`, `/jobadmin add <player> <amount>`, `/jobadmin remove <player> <amount>`, and `/jobadmin set <player> <amount>`.

---

More Minecraft plugin projects by EnderDevelopment: https://github.com/EnderDevelopment

Project page: https://dash.enderdevelopment.com/community/6fa0bea4-90c2-40c7-9f0e-006f9d81b0be?utm_source=github&utm_medium=readme&utm_campaign=auto_publish&utm_content=6fa0bea4-90c2-40c7-9f0e-006f9d81b0be
