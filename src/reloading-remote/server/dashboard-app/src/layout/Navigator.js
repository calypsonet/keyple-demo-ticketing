import React from 'react';
import PropTypes from 'prop-types';
import clsx from 'clsx';
import { withStyles } from '@mui/styles';
import Divider from '@mui/material/Divider';
import Drawer from '@mui/material/Drawer';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import PeopleIcon from '@mui/icons-material/People';
import DnsRoundedIcon from '@mui/icons-material/DnsRounded';
import SettingsInputComponentIcon from '@mui/icons-material/SettingsInputComponent';
import TimerIcon from '@mui/icons-material/Timer';
import SettingsIcon from '@mui/icons-material/Settings';
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

const styles = (theme) => ({
  categoryHeader: {
    paddingTop: theme.spacing(2),
    paddingBottom: theme.spacing(2),
  },
  categoryHeaderPrimary: {
    color: theme.palette.common.white,
  },
  item: {
    paddingTop: 1,
    paddingBottom: 1,
    color: 'rgba(255, 255, 255, 0.7)',
    '&:hover,&:focus': {
      backgroundColor: 'rgba(255, 255, 255, 0.08)',
    },
  },
  itemCategory: {
    //backgroundColor: '#232f3e',
    backgroundColor: '#1A87C7',
    //boxShadow: '0 -1px 0 #404854 inset',
    boxShadow: '0 -1px 0 #fff inset',
    paddingTop: theme.spacing(2),
    paddingBottom: theme.spacing(2),
  },
  keyple: {
    fontSize: 24,
    color: theme.palette.common.white,
  },
  itemActiveItem: {
    color: '#232f3e',
  },
  subtitle: {
    fontSize: 17,
    textAlign:'center'
  },
  itemPrimary: {
    fontSize: 'inherit',
  },
  itemIcon: {
    minWidth: 'auto',
    marginRight: theme.spacing(2),
  },
  divider: {
    marginTop: theme.spacing(2),
  },
});

function Navigator(props) {
  const { classes, ...other } = props;

  return (
    <Drawer variant="permanent" {...other}>
      <List disablePadding>
        <ListItem className={clsx(classes.keyple, classes.item, classes.itemCategory)}>
         <img src={logo} alt="Logo" style={{
           display:'block',
           marginLeft: 'auto',
           marginRight: 'auto'}}/>
        </ListItem>
        <ListItem className={clsx(classes.keyple,classes.item, classes.itemCategory)}>

          <ListItemText
            classes={{
              primary: classes.subtitle,
            }}
          >
            Open Source API for Smart Ticketing
          </ListItemText>
        </ListItem>
        {categories.map(({ id, children }) => (
          <React.Fragment key={id}>
            <ListItem className={classes.categoryHeader}>
              <ListItemText
                classes={{
                  primary: classes.categoryHeaderPrimary,
                }}
              >
                {id}
              </ListItemText>
            </ListItem>
            {children.map(({ id: childId, icon, active }) => (
              <ListItem
                key={childId}
                button
                className={clsx(classes.item, active && classes.itemActiveItem)}
              >
                <ListItemIcon className={classes.itemIcon}>{icon}</ListItemIcon>
                <ListItemText
                  classes={{
                    primary: classes.itemPrimary,
                  }}
                >
                  {childId}
                </ListItemText>
              </ListItem>
            ))}

            <Divider className={classes.divider} />
          </React.Fragment>
        ))}
      </List>
    </Drawer>
  );
}

Navigator.propTypes = {
  classes: PropTypes.object.isRequired,
};

export default withStyles(styles)(Navigator);