# Changelog - Version 1.0.6

## [2026-01-19] - Today's Changes

### Fixed
- **Player Shop Stock Bug**: Fixed issue where shop stock would increase even when the shop owner didn't have sufficient balance to pay for items sold to their shop. Stock now only increases after verifying the owner has enough money.
- **MyShop AddItem Item Removal**: Fixed issue where items were not being removed from player inventory when adding items to their personal shop. Simplified item removal logic to use `InventoryHelper.removeItem()` consistently.

### Changed
- **VaultUnlocked Plugin Identifier**: Updated VaultUnlocked plugin identifier check from `"TheEconomySystem:VaultUnlocked"` to `"TheEconomy:VaultUnlocked"`.

## [2026-01-18] - Yesterday's Changes

### Added
- **Notification System Integration**: Moved "You received $" reward messages to the notification system using `NotifyUtils` utility class. Rewards for breaking blocks and killing monsters now show as success notifications.
- **Hotbar Items Display**: Added hotbar items display in the "Add Item" window for both `/shop manager` and `/myshop manager` commands. Players can click on hotbar items to automatically fill the Item ID field. In `/myshop manager`, item quantities are also displayed on each hotbar item button.

### Fixed
- **Unknown Players in Leaderboard**: Fixed issue where some players appeared as "Desconhecido" (Unknown) in the "Top 10 Richest" leaderboard. The system now properly fetches player nicknames from JSON or Database storage.
- **Shop Manager Item Selection**: Fixed issue where clicking a hotbar item in `/shop manager` would change the quantity. Now only the Item ID is updated, preserving the existing quantity value.

### Changed
- **Polish Language Support**: Added full Polish (PL) language support to the translation system.

### Technical Improvements
- **VaultUnlocked Integration**: Improved VaultUnlocked detection using reflection, making it an optional dependency that doesn't break compilation if the JAR is missing.

