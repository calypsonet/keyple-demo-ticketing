import React, { useState, useEffect }  from 'react';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import Box from '@mui/material/Box';
import Navigator from './Navigator';
import Header from './Header';
import './App.css';
import useInterval from './util/useInterval'
import CollapsibleTable from './CollapsibleTable';
import Copyright from './Copyright'
import {layoutSx,drawerWidth,theme} from './util/theme'


export default function App() {
  const [mobileOpen, setMobileOpen] = useState(false);
  const [isSamReady, setIsSamReady] = useState(true);
  const [isServerReady, setIsServerReady] = useState(true);
  const [rows, setRows] = useState([]);
  const [lastRowId, setLastRowId] = useState();

  /*
   * Receives the transactions (executed once, when the component is mounted): the new transactions
   * are streamed by the server (Server-Sent Events), and the history is loaded at each
   * (re)connection, so that no transaction is missed while the connection was lost.
   */
  useEffect(() => {
    // Transactions received from the stream since the last (re)connection
    const streamed = [];

    const eventSource = new EventSource("/activity/stream");

    eventSource.onopen = () => {
      streamed.length = 0;
      fetch("/activity/events")
        .then(response => {
          if (!response.ok) {
            throw new Error("Response status: " + response.status);
          }
          return response.json();
        })
        .then(history => {
          // The history (oldest first) is authoritative, completed by the transactions streamed
          // after it was read. The table displays the most recent transactions first.
          const historyIds = new Set(history.map(transaction => transaction.id));
          const newer = streamed.filter(transaction => !historyIds.has(transaction.id));
          setRows([...newer].reverse().concat([...history].reverse()));
        })
        .catch(e => console.log("Error while loading the transactions: " + e));
    };

    eventSource.onmessage = event => {
      const transaction = JSON.parse(event.data);
      streamed.push(transaction);
      setLastRowId(transaction.id);
      setRows(rows => [transaction, ...rows]);
    };

    // The browser reconnects automatically
    eventSource.onerror = () => console.log("Transactions stream interrupted, reconnecting...");

    return () => eventSource.close();
  }, []);

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

  const drawerSlotProps = { paper: { style: { width: drawerWidth } } };

  return (
    <ThemeProvider theme={theme}>
      <Box sx={layoutSx.root}>
        <CssBaseline />
        <Box component="nav" sx={layoutSx.drawer}>
          {/* Mobile: temporary drawer (xs only) */}
          <Box sx={{ display: { xs: 'block', sm: 'none' } }}>
            <Navigator
              slotProps={drawerSlotProps}
              variant="temporary"
              open={mobileOpen}
              onClose={handleDrawerToggle}
            />
          </Box>
          {/* Desktop: permanent drawer (sm and above) */}
          <Box sx={{ display: { xs: 'none', sm: 'block' } }}>
            <Navigator slotProps={drawerSlotProps} />
          </Box>
        </Box>
        <Box sx={layoutSx.app}>
          <Header onDrawerToggle={handleDrawerToggle} isSamReady={isSamReady} isServerReady={isServerReady}/>
          <Box component="main" sx={layoutSx.main}>
            <CollapsibleTable rows={rows} lastRowId={lastRowId} />
          </Box>
          <Box component="footer" sx={layoutSx.footer}>
            <Copyright />
          </Box>
        </Box>
      </Box>
    </ThemeProvider>
  );
}
