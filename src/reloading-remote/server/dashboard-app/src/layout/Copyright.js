import Typography from '@mui/material/Typography';
import Link from '@mui/material/Link';
import logo_cna from '../img/logo-cna.png';

 export default function Copyright() {
  return (
    <div>
      <div align="center">
        <img src={logo_cna} width="100px" id="logo-calypso-networks-association" alt="Calypso Networks Association"/>
      </div>
      <Typography variant="body2" color="text.secondary" align="center">
        {'Eclipse Keyple © '}
        <Link color="inherit">
          Calypso Network Association
        </Link>{' '}
        {new Date().getFullYear()}
      </Typography>
    </div>
  );
}