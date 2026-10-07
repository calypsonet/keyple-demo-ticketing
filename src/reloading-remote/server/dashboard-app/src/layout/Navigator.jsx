import React from 'react';
import Divider from '@mui/material/Divider';
import Drawer from '@mui/material/Drawer';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import {
  People as PeopleIcon,
  DnsRounded as DnsRoundedIcon,
  SettingsInputComponent as SettingsInputComponentIcon,
  Timer as TimerIcon,
  Settings as SettingsIcon,
} from '@mui/icons-material';
import logo from '../img/logo.png';


const categories = [
  {
    id: 'Monitoring',
    children: [
      { id: 'Dashboard', icon: <PeopleIcon />},
      { id: 'Transactions', icon: <DnsRoundedIcon /> , active: true },
      { id: 'Sam Resources', icon: <SettingsInputComponentIcon /> },
    ],
  },
  {
    id: 'Card Utilities',
    children: [
      { id: 'Issuance', icon: <SettingsIcon /> },
      { id: 'Load', icon: <TimerIcon /> },
    ],
  },
];

// Header items of the drawer (logo and subtitle)
const headerItemSx = {
  fontSize: 24,
  color: 'common.white',
  backgroundColor: '#1A87C7',
  boxShadow: '0 -1px 0 #fff inset',
};

/**
 * Navigator Component (drawer)
 * @param props Drawer props
 * @returns {JSX.Element}
 * @constructor
 */
export default function Navigator(props) {
  return (
    <Drawer variant="permanent" {...props}>
      <List disablePadding>
        <ListItem sx={headerItemSx}>
         <img src={logo} alt="Logo" style={{
           display:'block',
           marginLeft: 'auto',
           marginRight: 'auto'}}/>
        </ListItem>
        <ListItem sx={headerItemSx}>
          <ListItemText slotProps={{ primary: { sx: { textAlign: 'center' } } }}>
            Open Source API for Smart Ticketing
          </ListItemText>
        </ListItem>
        {categories.map(({ id, children }) => (
          <React.Fragment key={id}>
            <ListItem>
              <ListItemText slotProps={{ primary: { sx: { color: 'common.white' } } }}>
                {id}
              </ListItemText>
            </ListItem>
            {children.map(({ id: childId, icon }) => (
              <ListItemButton key={childId}>
                <ListItemIcon>{icon}</ListItemIcon>
                <ListItemText>{childId}</ListItemText>
              </ListItemButton>
            ))}

            <Divider />
          </React.Fragment>
        ))}
      </List>
    </Drawer>
  );
}
