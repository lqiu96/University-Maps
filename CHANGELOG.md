# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [0.1.2] - 2016-01-05
### Changed
- Data from Twitter and Facebook is split into two files.
- Added :gen-class to be run in a java project
- Allowed the user to specify how many results they want back
- Added functionality to be able to search the directory for
the 'UnivDataInfo.csv' and 'WordSentiment.csv' files
- Added multi-arity (get-data) function which sets num-result as default of 10
- Allow user to choose how many tweets/post they want to get

## [Unreleased][unreleased]
### Changed
- Add a new arity to `make-widget-async` to provide a different widget shape.

## [0.1.1] - 2016-01-02
### Changed
- Documentation on how to make the widgets.

### Removed
- `make-widget-sync` - we're all async, all the time.

### Fixed
- Fixed widget maker to keep working when daylight savings switches over.

## 0.1.0 - 2016-01-02
### Added
- Files from the new template.
- Widget maker public API - `make-widget-sync`.

[unreleased]: https://github.com/your-name/university-maps/compare/0.1.1...HEAD
[0.1.1]: https://github.com/your-name/university-maps/compare/0.1.0...0.1.1
