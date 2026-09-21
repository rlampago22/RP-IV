package br.edu.unipampa.usina.apiestado;

/** Contadores EDA exibidos no T01 (medições, alarmes emitidos, eventos auditados). */
public record Contadores(int medicoes, int alarmes, int auditoria) {}
