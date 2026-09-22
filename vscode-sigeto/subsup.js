'use strict';

/**
 * A markdown-it rule for sigeto's "~subscript~" and "^superscript^", built to
 * agree with what the generator's SubSupExtension does, so the VS Code preview
 * shows what the site will show:
 *
 * - one marker only; "~~x~~" is strikethrough and is left to that rule,
 * - the content may not hold whitespace, which is what keeps running text like
 *   "install it under ~/opt ... ~/opt/ti-cgt-pru" from turning into one big
 *   subscript,
 * - the markers must be able to open and close by the same flanking rules
 *   commonmark applies to "*", which is what commonmark-java applies to a
 *   custom delimiter processor. scanDelims() with canSplitWord is markdown-it's
 *   own implementation of those rules, so "H~2~O" works while "x^*i*^" does
 *   not - exactly as in the generator.
 */
function subSupRule(marker, tag) {
	const markerCode = marker.charCodeAt(0);

	return function(state, silent) {
		const start = state.pos;
		const max = state.posMax;

		if(state.src.charCodeAt(start) !== markerCode)
			return false;
		if(silent)												// A marker alone never stands for anything
			return false;
		if(start + 2 >= max)									// No room for "~x~"
			return false;

		const opening = state.scanDelims(start, true);
		if(opening.length !== 1 || !opening.can_open)
			return false;

		//-- Find the closing marker, stepping over whatever inline constructs sit in between
		let closer = -1;
		state.pos = start + 1;
		while(state.pos < max) {
			if(state.src.charCodeAt(state.pos) === markerCode) {
				const closing = state.scanDelims(state.pos, true);
				if(closing.length === 1 && closing.can_close) {
					closer = state.pos;
				}
				break;
			}
			state.md.inline.skipToken(state);
		}
		state.pos = start;
		if(closer < 0)
			return false;

		const content = state.src.slice(start + 1, closer);
		if(content.length === 0 || /\s/.test(content))
			return false;

		state.pos = start + 1;
		state.posMax = closer;
		state.push(tag + '_open', tag, 1).markup = marker;
		state.md.inline.tokenize(state);
		state.push(tag + '_close', tag, -1).markup = marker;
		state.pos = closer + 1;
		state.posMax = max;
		return true;
	};
}

/** The markdown-it plugin: adds both rules. */
function subSupPlugin(md) {
	md.inline.ruler.after('emphasis', 'sigeto_sub', subSupRule('~', 'sub'));
	md.inline.ruler.after('emphasis', 'sigeto_sup', subSupRule('^', 'sup'));
}

module.exports = subSupPlugin;
