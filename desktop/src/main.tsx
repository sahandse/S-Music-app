import React from 'react'
import ReactDOM from 'react-dom/client'
import { App } from './App'
import { AppProvider } from './store'
import { applyTheme } from './settings'
import './styles.css'

applyTheme()

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <AppProvider>
      <App />
    </AppProvider>
  </React.StrictMode>
)
