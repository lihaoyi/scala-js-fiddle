/** @constructor */
ScalaJS.c.Main$ = (function() {
  ScalaJS.c.java_lang_Object.call(this)
});
ScalaJS.c.Main$.prototype = new ScalaJS.inheritable.java_lang_Object();
ScalaJS.c.Main$.prototype.constructor = ScalaJS.c.Main$;
ScalaJS.c.Main$.prototype.main__AT__V = (function(args) {
  var a = 2;
  var b = 3;
  ScalaJS.modules.scala_Predef().println__O__V(ScalaJS.bI((a + b)))
});
ScalaJS.c.Main$.prototype.main = (function(arg$1) {
  return this.main__AT__V(arg$1)
});
ScalaJS.c.Main$.prototype.main__AT__ = (function(args) {
  return ScalaJS.bV(this.main__AT__V(args))
});
/** @constructor */
ScalaJS.inheritable.Main$ = (function() {
  /*<skip>*/
});
ScalaJS.inheritable.Main$.prototype = ScalaJS.c.Main$.prototype;
/** @constructor */
ScalaJS.classes.Main$ = (function() {
  ScalaJS.c.Main$.call(this);
  return this.init___()
});
ScalaJS.classes.Main$.prototype = ScalaJS.c.Main$.prototype;
ScalaJS.is.Main$ = (function(obj) {
  return (!(!((obj && obj.$classData) && obj.$classData.ancestors.Main$)))
});
ScalaJS.as.Main$ = (function(obj) {
  if ((ScalaJS.is.Main$(obj) || (obj === null))) {
    return obj
  } else {
    ScalaJS.throwClassCastException(obj, "Main")
  }
});
ScalaJS.isArrayOf.Main$ = (function(obj, depth) {
  return (!(!(((obj && obj.$classData) && (obj.$classData.arrayDepth === depth)) && obj.$classData.arrayBase.ancestors.Main$)))
});
ScalaJS.asArrayOf.Main$ = (function(obj, depth) {
  if ((ScalaJS.isArrayOf.Main$(obj, depth) || (obj === null))) {
    return obj
  } else {
    ScalaJS.throwArrayCastException(obj, "LMain;", depth)
  }
});
ScalaJS.data.Main$ = new ScalaJS.ClassTypeData({
  Main$: 0
}, false, "Main$", ScalaJS.data.java_lang_Object, {
  Main$: 1,
  java_lang_Object: 1
});
ScalaJS.c.Main$.prototype.$classData = ScalaJS.data.Main$;
ScalaJS.moduleInstances.Main = undefined;
ScalaJS.modules.Main = (function() {
  if ((!ScalaJS.moduleInstances.Main)) {
    ScalaJS.moduleInstances.Main = new ScalaJS.c.Main$().init___()
  };
  return ScalaJS.moduleInstances.Main
});
//@ sourceMappingURL=0002-Main$.js.map
