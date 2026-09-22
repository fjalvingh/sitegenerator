'use strict';

const subSupPlugin = require('./subsup');

function activate() {
	return {
		extendMarkdownIt(md) {
			return md.use(subSupPlugin);
		}
	};
}

function deactivate() {
}

module.exports = {activate, deactivate};
