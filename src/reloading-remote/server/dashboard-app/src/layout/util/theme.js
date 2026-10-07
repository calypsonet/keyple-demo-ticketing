import background from '../../img/background.png';
import { createTheme } from '@mui/material/styles';

// Create base theme
const muitheme = createTheme({
  palette: {
    primary: {
      main: '#fff',
    },
  },
  typography: {
    fontFamily: 'Work Sans',
    h5: {
      fontWeight: 500,
      fontSize: 26,
      letterSpacing: 0.5,
    },
  },
  shape: {
    borderRadius: 8,
  },
  mixins: {
    toolbar: {
      minHeight: 48,
    },
  },
});

// Extend with overrides of the components used by the dashboard
export const theme = createTheme(muitheme, {
  components: {
    MuiDrawer: {
      styleOverrides: {
        paper: {
          backgroundColor: '#1A87C7',
        },
      },
    },
    MuiIconButton: {
      styleOverrides: {
        root: {
          padding: muitheme.spacing(1),
        },
      },
    },
    MuiDivider: {
      styleOverrides: {
        root: {
          backgroundColor: '#fff',
        },
      },
    },
    MuiListItemText: {
      styleOverrides: {
        primary: {
          fontWeight: muitheme.typography.fontWeightMedium,
        },
      },
    },
    MuiListItemIcon: {
      styleOverrides: {
        root: {
          color: 'inherit',
          // Material UI v9 default is 36px
          minWidth: 56,
          marginRight: 0,
          '& svg': {
            fontSize: 20,
          },
        },
      },
    },
    MuiAvatar: {
      styleOverrides: {
        root: {
          width: 32,
          height: 32,
        },
      },
    },
  },
});

export const drawerWidth = 200;

// Layout styles (sx prop)
export const layoutSx = {
  root: {
    display: 'flex',
    minHeight: '100vh',
  },
  drawer: {
    width: { sm: drawerWidth },
    flexShrink: { sm: 0 },
  },
  app: {
    flex: 1,
    display: 'flex',
    flexDirection: 'column',
  },
  main: {
    flex: 1,
    py: 3,
    px: 4,
    background: '#fff',
    backgroundImage: `url(${background})`,
    backgroundRepeat: 'no-repeat',
    backgroundSize: 'cover',
  },
  footer: {
    p: 2,
    background: '#fff',
  },
};
