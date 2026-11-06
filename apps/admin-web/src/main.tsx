import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { Foundation, Scope } from './features/foundation/Foundation';
import { Layout } from './shared/Layout';
import './style.css';

ReactDOM.createRoot(document.getElementById('root')!).render(<React.StrictMode><BrowserRouter><Routes><Route element={<Layout />}><Route index element={<Foundation />}/><Route path="scope" element={<Scope />}/><Route path="*" element={<h1>Không tìm thấy trang</h1>}/></Route></Routes></BrowserRouter></React.StrictMode>);
