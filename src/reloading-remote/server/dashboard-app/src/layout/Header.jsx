import React from 'react';
import AppBar from '@mui/material/AppBar';
import Avatar from '@mui/material/Avatar';
import Grid from '@mui/material/Grid';
import IconButton from '@mui/material/IconButton';
import { Menu as MenuIcon } from '@mui/icons-material';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import AlertDialog from './AlertDialog'

/**
 * Header Component
 * @param props onDrawerToggle (function), isSamReady (boolean), isServerReady (boolean)
 * @returns {JSX.Element}
 * @constructor
 */
export default function Header(props) {
  const { onDrawerToggle, isSamReady, isServerReady } = props;
  return (
    <React.Fragment>
      <AlertDialog show={isServerReady && !isSamReady} title="SAM Resource is not available" text="Please ensure that the SAM is inserted into the SAM Reader"/>
      <AlertDialog show={!isServerReady} title="Can not reach Keyple Distributed Server" text="Keyple Distributed server is not reachable. Please ensure that the Server process is running. Restart it if needed."/>
      <AppBar color="primary" position="sticky" elevation={0}>
        <Toolbar>
          <Grid container spacing={1} sx={{ alignItems: 'center', flex: 1 }}>
            <Grid sx={{ display: { xs: 'flex', sm: 'none' } }}>
              <IconButton
                color="inherit"
                aria-label="open drawer"
                onClick={onDrawerToggle}
              >
                <MenuIcon />
              </IconButton>
            </Grid>
            <Grid size="grow" />
            <Grid>
              <Typography>
                {isSamReady ? "Sam Resource is ready" : "Sam Resource is NOT Ready"}
              </Typography>
            </Grid>
            <Grid>
              <IconButton color="inherit">
                <Avatar alt="My Avatar" />
              </IconButton>
            </Grid>
          </Grid>
        </Toolbar>
      </AppBar>
      <AppBar
        component="div"
        sx={{ zIndex: 0 }}
        color="primary"
        position="static"
        elevation={0}
      >
        <Toolbar>
          <Grid container spacing={2} sx={{ alignItems: 'center' }}>
            <Grid />
            <Grid>
              <Typography color="inherit" variant="h5" component="h1">
                Ticketing Transactions
              </Typography>
            </Grid>
          </Grid>
        </Toolbar>
      </AppBar>
    </React.Fragment>
  );
}
