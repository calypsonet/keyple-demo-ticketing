import React from 'react';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Paper from '@mui/material/Paper';

// Number of columns of the table
const COLUMN_COUNT = 7;

function Row(props) {
  const { row, lastRowId } = props;

  return (
    <TableRow className={row.id === lastRowId ? (row.status === "SUCCESS" ? `newRowSuccess` : `newRowError`) : undefined}>
      <TableCell align="center">{row.id.substring(0, 4)}</TableCell>
      <TableCell align="center">{row.startedAt}</TableCell>
      <TableCell align="center">{row.plugin}</TableCell>
      <TableCell align="center">{row.type}</TableCell>
      <TableCell align="center">{row.cardSerialNumber}</TableCell>
      <TableCell align="center">{row.status}</TableCell>
      <TableCell align="center">{row.contractLoaded}</TableCell>
    </TableRow>
  );
}

export default function TransactionTable(props) {
  const { rows, lastRowId } = props;

  return (
    <TableContainer component={Paper}>
      <Table aria-label="transactions table">
        <TableHead>
          <TableRow>
            <TableCell align="center">ID</TableCell>
            <TableCell align="center">Started At</TableCell>
            <TableCell align="center">Plugin</TableCell>
            <TableCell align="center">Type</TableCell>
            <TableCell align="center">Card S/N</TableCell>
            <TableCell align="center">Status</TableCell>
            <TableCell align="center">Contract Loaded</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.length > 0 ?
            rows.map((row) => (<Row key={row.id} row={row} lastRowId={lastRowId} />))
            : <TableRow key="empty-row"><TableCell colSpan={COLUMN_COUNT}>No transaction has been processed yet</TableCell></TableRow>}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
