import { h } from 'preact';
import { Overview } from './Overview.jsx';
import { Servers } from './Servers.jsx';
import { Server } from './Server.jsx';
import { Templates } from './Templates.jsx';
import { Template } from './Template.jsx';
import { Players } from './Players.jsx';
import { Nodes } from './Nodes.jsx';
import { Schedules } from './Schedules.jsx';
import { Backups } from './Backups.jsx';
import { Groups } from './Groups.jsx';
import { Maintenance } from './Maintenance.jsx';
import { Plugins } from './Plugins.jsx';
import { CloudConsole } from './CloudConsole.jsx';
import { Crashes } from './Crashes.jsx';
import { Users } from './Users.jsx';
import { t } from '../i18n.js';

// list page, or the detail page when the route has a name: #/servers/Lobby-1
const listOrDetail = (List, Detail) => ({ params, session }) =>
  params[0] ? h(Detail, { name: params[0], key: params[0], session }) : h(List, { session });

// '' is the overview; the order here is the order in the sidebar
export const pages = {
  '': { label: 'Overview', component: Overview },
  console: { label: 'Console', component: CloudConsole },
  servers: { label: 'Servers', component: listOrDetail(Servers, Server) },
  templates: { label: 'Templates', component: listOrDetail(Templates, Template) },
  groups: { label: 'Groups', component: Groups },
  players: { label: 'Players', component: Players },
  maintenance: { label: 'Maintenance', component: Maintenance },
  nodes: { label: 'Nodes', component: Nodes },
  schedules: { label: 'Schedules', component: Schedules },
  backups: { label: 'Backups', component: Backups },
  plugins: { label: 'Plugins', component: Plugins },
  crashes: { label: 'Crashes', component: Crashes },
  users: { label: 'Users', component: Users, admin: true },
};

export const NotFound = () => h('section', null, h('h1', null, t('Not found')), h('a', { href: '#/' }, t('Back to the overview')));
