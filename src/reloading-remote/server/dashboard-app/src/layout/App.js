import React, { useState, useEffect }  from 'react';
import PropTypes from 'prop-types';
import { ThemeProvider } from '@mui/material/styles';
import { ThemeProvider as StylesThemeProvider, withStyles } from '@mui/styles';
import CssBaseline from '@mui/material/CssBaseline';
import Box from '@mui/material/Box';
import Navigator from './Navigator';
import Header from './Header';
import './App.css';
import useInterval from './util/useInterval'
import CollapsibleTable from './CollapsibleTable';
import Copyright from './Copyright'
import {styles,drawerWidth,theme} from './util/theme'


function Paperbase(props) {
  const { classes } = props;
  const [mobileOpen, setMobileOpen] = useState(false);
  const [isSamReady, setIsSamReady] = useState(true);
  const [isServerReady, setIsServerReady] = useState(true);
  const [rows, setRows] = useState([]);
  const [lastRowId, setLastRowId] = useState();
  const [shouldPoll, setShouldPoll] = useState(1);

  /*
   * Use Effect Hook to long poll a new transaction
    * useEffect will executed only once when component is mounted
   */

  useEffect(() => {

    //activate transaction polling
    function activateTransactionPoll(handleNewTransaction) {
      fetch("/activity/events/wait")
        .then(response => {
          if (response.status === 204) {
            console.log("Received a No-Content response from server: " + response.status);
            //return null;
          } else if (response.status === 200) {
            // Received a new transaction
            return response.json()
          } else {
            //unexpected error connect again
            throw new Error("Exception, response status : "+ response.status)
          }
        })
        .then((json)=>{
          if(json){
            handleNewTransaction(json);
          }
          console.log("Polling iteration: " + shouldPoll);
          setShouldPoll(shouldPoll+1);//update shouldPoll value to rerun the useEffect
        })
        .catch(e =>{
          console.log("Error while connection to server : "+ e)
          setTimeout(()=>{
            console.log("Timeout polling iteration : " + shouldPoll);
            setShouldPoll(shouldPoll+1)//update shouldPoll value to rerun the useEffect
          },5000);
        })
    }

    function handleNewTransaction(transaction){
        setLastRowId(transaction.id);
        console.log("Adding a new row to transaction table: " + JSON.stringify(transaction));
        setRows(rows =>[
          transaction,
          ...rows
        ]);
    }

    //activate
    activateTransactionPoll(handleNewTransaction);
    },
    //use effect when should poll value is updated
    [shouldPoll]);

  /*
   * Use Custom Interval Hook to poll SAM and Server state
   */
  useInterval(() => {
    /*
     * Fetch the server/sam state and change server/sam status
     */
    function fetchIsSamReady () {
      var requestOptions = {
        method: 'GET'
      };

      //request SAM status
      fetch("/card/sam-status", requestOptions)
        .then(response => {
          if(response.status === 200){
            //update server state if required
            if(!isServerReady){
              setIsServerReady(true);
            }
            return response.json()
          }else{
            throw new Error('Request status : ' + response.status);
          }
        })
        .then(json => {
          console.log("Is Sam Ready : " + json.isSamReady);
          //update sam state if required
          if(isSamReady !== json.isSamReady){
            setIsSamReady(json.isSamReady)
          }
        })
        .catch(error =>{
          console.log('Fetch error', error)
          //update server state if required
          if(isServerReady){
            setIsServerReady(false)
          }

        });
    }

    fetchIsSamReady()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, 3000);

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen);
  };

  return (
    <ThemeProvider theme={theme}>
      <StylesThemeProvider theme={theme}>
        <div className={classes.root}>
          <CssBaseline />
          <nav className={classes.drawer}>
            {/* Mobile: temporary drawer (xs only) */}
            <Box sx={{ display: { xs: 'block', sm: 'none' } }}>
              <Navigator
                PaperProps={{ style: { width: drawerWidth } }}
                variant="temporary"
                open={mobileOpen}
                onClose={handleDrawerToggle}
              />
            </Box>
            {/* Desktop: permanent drawer (sm and above) */}
            <Box sx={{ display: { xs: 'none', sm: 'block' } }}>
              <Navigator PaperProps={{ style: { width: drawerWidth } }} />
            </Box>
          </nav>
          <div className={classes.app}>
            <Header onDrawerToggle={handleDrawerToggle} isSamReady={isSamReady} isServerReady={isServerReady}/>
            <main className={classes.main}>
              <CollapsibleTable rows={rows} lastRowId={lastRowId} />
            </main>
            <footer className={classes.footer}>
              <Copyright />
            </footer>
          </div>
        </div>
      </StylesThemeProvider>
    </ThemeProvider>
  );
}

Paperbase.propTypes = {
  classes: PropTypes.object.isRequired,
};

export default withStyles(styles)(Paperbase);
