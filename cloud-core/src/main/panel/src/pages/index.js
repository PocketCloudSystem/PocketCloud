import { h } from 'preact';

const Soon = ({ title }) => () => h('section', null, h('h2', null, title), h('p', { class: 'muted' }, 'Coming soon.'));

// '' is the overview; the order here is the order in the sidebar
export const pages = {
  '': { label: 'Overview', component: Soon({ title: 'Overview' }) },
  servers: { label: 'Servers', component: Soon({ title: 'Servers' }) },
  templates: { label: 'Templates', component: Soon({ title: 'Templates' }) },
  players: { label: 'Players', component: Soon({ title: 'Players' }) },
  nodes: { label: 'Nodes', component: Soon({ title: 'Nodes' }) },
};

export const NotFound = () => h('section', null, h('h2', null, 'Not found'), h('a', { href: '#/' }, 'Back to the overview'));
