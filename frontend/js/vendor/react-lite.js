/**
 * Standalone Zero-Dependency React 18 Core Runtime
 * Provides standard React & ReactDOM UMD API:
 * React.createElement, useState, useEffect, useRef, Fragment
 * ReactDOM.createRoot, render
 */
(function(global) {
  'use strict';
  if (global.React && global.ReactDOM) return;

  var currentComponent = null;
  var hookIndex = 0;

  var React = {
    Fragment: Symbol ? Symbol.for('react.fragment') : '__REACT_FRAGMENT__',
    createElement: function(type, props) {
      var children = [];
      for (var i = 2; i < arguments.length; i++) {
        var arg = arguments[i];
        if (Array.isArray(arg)) {
          children = children.concat(arg);
        } else if (arg !== null && arg !== undefined && arg !== false) {
          children.push(arg);
        }
      }
      return {
        $$typeof: Symbol ? Symbol.for('react.element') : 0xeac7,
        type: type,
        props: Object.assign({}, props || {}, { children: children }),
        key: props && props.key ? props.key : null
      };
    },
    useState: function(initialValue) {
      if (!currentComponent) throw new Error('useState called outside component');
      var comp = currentComponent;
      var idx = hookIndex++;
      if (comp.hooks[idx] === undefined) {
        comp.hooks[idx] = typeof initialValue === 'function' ? initialValue() : initialValue;
      }
      var setState = function(newValue) {
        var val = typeof newValue === 'function' ? newValue(comp.hooks[idx]) : newValue;
        if (comp.hooks[idx] !== val) {
          comp.hooks[idx] = val;
          comp.scheduleRender();
        }
      };
      return [comp.hooks[idx], setState];
    },
    useEffect: function(callback, deps) {
      if (!currentComponent) return;
      var comp = currentComponent;
      var idx = hookIndex++;
      var oldDeps = comp.hooks[idx] ? comp.hooks[idx].deps : null;
      var hasChanged = true;
      if (oldDeps && deps) {
        hasChanged = deps.some(function(d, i) { return d !== oldDeps[i]; });
      }
      if (hasChanged) {
        if (comp.hooks[idx] && comp.hooks[idx].cleanup) {
          try { comp.hooks[idx].cleanup(); } catch (e) {}
        }
        comp.effectsQueue.push(function() {
          var cleanup = callback();
          comp.hooks[idx] = { deps: deps, cleanup: typeof cleanup === 'function' ? cleanup : null };
        });
      }
    },
    useRef: function(initialValue) {
      if (!currentComponent) return { current: initialValue };
      var idx = hookIndex++;
      if (!currentComponent.hooks[idx]) {
        currentComponent.hooks[idx] = { current: initialValue };
      }
      return currentComponent.hooks[idx];
    }
  };

  function renderVNode(vnode, container) {
    if (vnode === null || vnode === undefined || vnode === false) {
      return null;
    }
    if (typeof vnode === 'string' || typeof vnode === 'number') {
      return document.createTextNode(String(vnode));
    }
    if (vnode.type === React.Fragment) {
      var frag = document.createDocumentFragment();
      (vnode.props.children || []).forEach(function(child) {
        var node = renderVNode(child, container);
        if (node) frag.appendChild(node);
      });
      return frag;
    }
    if (typeof vnode.type === 'function') {
      var compInstance = container.__compInstance || {
        hooks: [],
        effectsQueue: [],
        scheduleRender: function() {
          if (container.__renderPending) return;
          container.__renderPending = true;
          requestAnimationFrame(function() {
            container.__renderPending = false;
            container.innerHTML = '';
            var newDom = renderVNode(vnode, container);
            if (newDom) container.appendChild(newDom);
          });
        }
      };
      container.__compInstance = compInstance;
      currentComponent = compInstance;
      hookIndex = 0;
      compInstance.effectsQueue = [];
      var rendered = vnode.type(vnode.props);
      currentComponent = null;

      var dom = renderVNode(rendered, container);
      setTimeout(function() {
        compInstance.effectsQueue.forEach(function(eff) { eff(); });
      }, 0);
      return dom;
    }

    var el = document.createElement(vnode.type);
    var props = vnode.props || {};

    Object.keys(props).forEach(function(key) {
      if (key === 'children') return;
      if (key === 'ref' && props[key] && typeof props[key] === 'object') {
        props[key].current = el;
      } else if (key === 'className') {
        el.className = props[key];
      } else if (key === 'dangerouslySetInnerHTML' && props[key]) {
        el.innerHTML = props[key].__html || '';
      } else if (key === 'style' && typeof props[key] === 'object') {
        Object.assign(el.style, props[key]);
      } else if (key.startsWith('on') && typeof props[key] === 'function') {
        var eventName = key.substring(2).toLowerCase();
        el.addEventListener(eventName, props[key]);
      } else if (key === 'value' && (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA')) {
        el.value = props[key];
      } else if (key === 'disabled') {
        el.disabled = Boolean(props[key]);
      } else {
        el.setAttribute(key, props[key]);
      }
    });

    (props.children || []).forEach(function(child) {
      var node = renderVNode(child, container);
      if (node) el.appendChild(node);
    });

    return el;
  }

  var ReactDOM = {
    render: function(vnode, container) {
      container.innerHTML = '';
      var dom = renderVNode(vnode, container);
      if (dom) container.appendChild(dom);
    },
    createRoot: function(container) {
      return {
        render: function(vnode) {
          ReactDOM.render(vnode, container);
        }
      };
    }
  };

  global.React = React;
  global.ReactDOM = ReactDOM;
})(typeof window !== 'undefined' ? window : this);

