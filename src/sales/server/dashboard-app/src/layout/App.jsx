import React, { useState, useEffect }  from 'react';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import Box from '@mui/material/Box';
import Navigator from './Navigator';
import Header from './Header';
import './App.css';
import useInterval from './util/useInterval'
import TransactionTable from './TransactionTable';
import Copyright from './Copyright'
import {layoutSx,drawerWidth,theme} from './util/theme'


export default function App() {
  const [mobileOpen, setMobileOpen] = useState(false);
  const [isSamAvailable, setIsSamAvailable] = useState(true);
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

    const eventSource = new EventSource("/transactions/stream");

    eventSource.onopen = () => {
      streamed.length = 0;
      fetch("/transactions")
        .then(response => {
          if (!response.ok) {
            throw new Error("Unexpected response [status=" + response.status + "]");
          }
          return response.json();
        })
        .then(history => {
          // The history (oldest first) is authoritative, completed by the transactions streamed
          // after it was read. The table displays the most recent transactions first.
          const historyIds = new Set(history.map(transaction => transaction.id));
          const missedTransactions = streamed.filter(transaction => !historyIds.has(transaction.id));
          setRows([...missedTransactions].reverse().concat([...history].reverse()));
        })
        .catch(error => console.error("Error while loading the transactions [cause=" + error.message + "]"));
    };

    eventSource.onmessage = event => {
      const transaction = JSON.parse(event.data);
      streamed.push(transaction);
      setLastRowId(transaction.id);
      setRows(rows => [transaction, ...rows]);
    };

    // The browser reconnects automatically
    eventSource.onerror = () => console.warn("Transactions stream interrupted, reconnecting");

    return () => eventSource.close();
  }, []);

  /*
   * Use Custom Interval Hook to poll SAM and Server state
   */
  useInterval(() => {
    /*
     * Fetches the availability of the SAM, which also gives the readiness of the server
     */
    function fetchSamAvailability () {
      var requestOptions = {
        method: 'GET'
      };

      fetch("/card/sam-availability", requestOptions)
        .then(response => {
          if(response.status === 200){
            // Only the changes of the readiness are logged, the server being checked periodically
            if(!isServerReady){
              console.info("Server ready");
              setIsServerReady(true);
            }
            return response.json()
          }else{
            throw new Error("Unexpected response [status=" + response.status + "]");
          }
        })
        .then(samAvailability => {
          // Only the changes of the availability of the SAM are logged
          if(isSamAvailable !== samAvailability.isSamAvailable){
            console.info("SAM availability changed [available=" + samAvailability.isSamAvailable + "]");
            setIsSamAvailable(samAvailability.isSamAvailable)
          }
        })
        .catch(error =>{
          if(isServerReady){
            console.warn("Server not ready [cause=" + error.message + "]");
            setIsServerReady(false)
          }

        });
    }

    fetchSamAvailability()
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
          <Header onDrawerToggle={handleDrawerToggle} isSamAvailable={isSamAvailable} isServerReady={isServerReady}/>
          <Box component="main" sx={layoutSx.main}>
            <TransactionTable rows={rows} lastRowId={lastRowId} />
          </Box>
          <Box component="footer" sx={layoutSx.footer}>
            <Copyright />
          </Box>
        </Box>
      </Box>
    </ThemeProvider>
  );
}
