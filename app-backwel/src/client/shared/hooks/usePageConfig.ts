import { useState, useEffect, useCallback } from 'react';

export type PageConfigSection = Record<string, any>;
export type PageConfig = Record<string, PageConfigSection>;

export function usePageConfig() {
  const [publishedConfig, setPublishedConfig] = useState<PageConfig | null>(null);
  const [draftConfig, setDraftConfig] = useState<PageConfig | null>(null);
  const [history, setHistory] = useState<PageConfig[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    fetch('/api/config/page')
      .then((res) => {
        if (!res.ok) throw new Error();
        return res.json();
      })
      .then((data) => {
        setPublishedConfig(data);
        setIsLoading(false);
      })
      .catch(() => {
        setIsLoading(false);
      });

    const savedDraft = localStorage.getItem('backwel_draft_config');
    if (savedDraft) {
      try {
        setDraftConfig(JSON.parse(savedDraft));
      } catch (e) {}
    }
  }, []);

  const updateDraft = useCallback((section: string, changes: PageConfigSection) => {
    setDraftConfig((prev) => {
      const current = prev || publishedConfig || {};
      setHistory((h) => [...h, current]);
      
      const updated = {
        ...current,
        [section]: {
          ...(current[section] || {}),
          ...changes
        }
      };
      
      localStorage.setItem('backwel_draft_config', JSON.stringify(updated));
      return updated;
    });
  }, [publishedConfig]);

  const undoLastChange = useCallback(() => {
    setHistory((h) => {
      if (h.length === 0) return h;
      const newHistory = [...h];
      const previous = newHistory.pop()!;
      
      setDraftConfig(previous);
      localStorage.setItem('backwel_draft_config', JSON.stringify(previous));
      return newHistory;
    });
  }, []);

  const clearAllDrafts = useCallback(() => {
    setDraftConfig(null);
    setHistory([]);
    localStorage.removeItem('backwel_draft_config');
  }, []);

  const publishChanges = useCallback(async () => {
    if (!draftConfig) return false;
    
    try {
      const promises = Object.entries(draftConfig).map(([section, config]) => 
        fetch('/api/config/page', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ section, config })
        })
      );
      
      await Promise.all(promises);
      
      setPublishedConfig(draftConfig);
      clearAllDrafts();
      return true;
    } catch (error) {
      return false;
    }
  }, [draftConfig, clearAllDrafts]);

  const createSavepoint = useCallback(() => {
    const current = draftConfig || publishedConfig || {};
    localStorage.setItem('backwel_savepoint', JSON.stringify(current));
    alert('Savepoint creado con éxito.');
  }, [draftConfig, publishedConfig]);

  const restoreSavepoint = useCallback(() => {
    const saved = localStorage.getItem('backwel_savepoint');
    if (saved) {
      const parsed = JSON.parse(saved);
      setHistory((h) => [...h, draftConfig || publishedConfig || {}]);
      setDraftConfig(parsed);
      localStorage.setItem('backwel_draft_config', saved);
    } else {
      alert('No hay ningún savepoint guardado.');
    }
  }, [draftConfig, publishedConfig]);
  return {
    publishedConfig,
    draftConfig,
    isLoading,
    updateDraft,
    undoLastChange,
    clearAllDrafts,
    publishChanges,
    createSavepoint,
    restoreSavepoint
  };
}