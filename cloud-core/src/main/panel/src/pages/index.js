import { h } from 'preact';
import { Overview } from './Overview.jsx';
import { Servers } from './Servers.jsx';
import { Server } from './Server.jsx';

const Soon = ({ title }) => () => h('section', null, h('h1', null, title), h('p', { class: 'muted' }, 'Coming soon.'));

// '' is the overview; the order here is the order in the sidebar
export const pages = {
  '': { label: 'Overview', component: Overview },
  servers: { label: 'Servers', component: ({ params }) => (params[0] ? h(Server, { name: params[0], key: params[0] }) : h(Servers)) },
  templates: { label: 'Templates', component: Soon({ title: 'Templates' }) },
  players: { label: 'Players', component: Soon({ title: 'Players' }) },
  nodes: { label: 'Nodes', component: Soon({ title: 'Nodes' }) },
};

export const NotFound = () => h('section', null, h('h1', null, 'Not found'), h('a', { href: '#/' }, 'Back to the overview'));
