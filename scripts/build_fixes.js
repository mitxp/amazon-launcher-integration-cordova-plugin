const hook_tools = require('./hook_tools');

// Run fixes needed during build, needs to run just before cordova compile (after cordova prepare)
module.exports = function (context) {
    hook_tools.fixActivityParent(context);
};
